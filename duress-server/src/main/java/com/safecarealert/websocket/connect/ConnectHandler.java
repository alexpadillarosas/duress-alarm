package com.safecarealert.websocket.connect;

import com.safecarealert.alerts.GroupKey;
import com.safecarealert.alerts.GroupState;
import com.safecarealert.alerts.registry.GroupRegistry;
import com.safecarealert.core.ClientType;
import com.safecarealert.core.ConnectMessage;
import com.safecarealert.core.TerminationReason;
import com.safecarealert.dashboard.DeviceStatus;
import com.safecarealert.messages.DeviceMessage;
import com.safecarealert.messages.MessageType;
import com.safecarealert.utils.JsonService;
import com.safecarealert.websocket.AlertSocket;
import com.safecarealert.websocket.ConnectionEvents;
import com.safecarealert.websocket.DeviceContext;
import com.safecarealert.websocket.WebSocketIdentityValidator;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.websockets.next.CloseReason;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Map;

/**
 * ConnectHandler
 * --------------
 * Central handler for the initial CONNECT handshake from both Devices and Monitors.
 * <p>
 * This class now cleanly separates the logic for DEVICE vs MONITOR using the
 * new sealed ConnectionContext architecture.
 * <p>
 * Responsibilities:
 *   - Validate identity vs CONNECT payload
 *   - Bind appropriate context (DeviceContext or MonitorContext)
 *   - Register device in GroupRegistry (only for devices)
 *   - Send DEVICE_CONNECTED ACK + replay active alerts (only for devices)
 */
@ApplicationScoped
public class ConnectHandler {

    @Inject Logger log;
    @Inject WebSocketIdentityValidator validator;
    @Inject GroupRegistry registry;
    @Inject JsonService jsonService;
    @Inject ConnectionEvents connectionEvents;

    @Inject
    @ConfigProperty(name = "app.version")
    String appVersion;

    /**
     * Main entry point for CONNECT messages.
     * <p>
     * Note: Monitors and Devices use different WebSocket endpoints:
     *   - Devices → /ws/v1/alerts
     *   - Monitors → /ws/v1/monitor
     * <p>
     * Even though both send a CONNECT message, they are handled differently.
     */
    public Uni<Void> handle(WebSocketConnection connection, ConnectMessage msg, SecurityIdentity identity) {

        log.infov("🔌 CONNECT received: clientType={0}, subject={1}, tenant={2}",
                msg.clientType(), msg.subjectId(), msg.tenantId());

        // =============================================================
        // MONITOR CONNECT
        // =============================================================
        if (msg.clientType() == ClientType.MONITOR) {
            /*
             * Why we delegate / return early:
             *
             *    Monitors have their own dedicated endpoint (/ws/v1/monitor)
             *    and their own socket class (MonitorSocket).
             *
             *    Monitor connection logic (subscription handling, hydration, etc.)
             *    is completely different from devices.
             *
             *    We don't want to register monitors in GroupRegistry or create
             *    DeviceContext, because they don't belong to any specific group.
             *
             *    The actual monitor CONNECT handling is done in MonitorSocket.handleConnect()
             */
            log.infov("🖥️ MONITOR CONNECT - delegating to MonitorSocket");
            return Uni.createFrom().voidItem();
        }

        // =============================================================
        // DEVICE CONNECT (Original logic, now with new context)
        // =============================================================
        String tenantId    = msg.tenantId();
        String workplaceId = msg.workplaceId();
        String groupId     = msg.groupId();
        String deviceId    = msg.subjectId();

        // Identity validation (strict for devices)
        if (!validator.validateDeviceContext(identity, connection, tenantId, workplaceId, groupId, deviceId)) {
            log.warnf("🔓 DEVICE_REJECTED: Identity mismatch for device={0}", deviceId);
            return connection.close(new CloseReason(
                    TerminationReason.FORBIDDEN.getCode(),
                    TerminationReason.FORBIDDEN.getDescription()
            ));
        }

        log.infov("🔐 DEVICE_AUTHENTICATED: device={0}, tenant={1}, workplace={2}, group={3}",
                deviceId, tenantId, workplaceId, groupId);

        // Bind DeviceContext to connection
        DeviceContext deviceContext = new DeviceContext(tenantId, workplaceId, groupId, deviceId);

        // Store keys for backward compatibility with existing code
        connection.userData().put(AlertSocket.DEVICE_ID, deviceId);
        connection.userData().put(AlertSocket.TENANT_ID, tenantId);
        connection.userData().put(AlertSocket.WORKPLACE_ID, workplaceId);
        connection.userData().put(AlertSocket.GROUP_ID, groupId);

        connectionEvents.onDeviceConnected(connection, deviceId);

        // Register in group state
        GroupKey groupKey = new GroupKey(tenantId, workplaceId, groupId);
        GroupState state = registry.getOrCreateState(groupKey);

        state.connectedDeviceIds.add(deviceId);
        registry.registerDevice(groupKey, deviceId, connection);

        // Send DEVICE_CONNECTED acknowledgment
        DeviceMessage ack = new DeviceMessage(
                MessageType.DEVICE_CONNECTED,
                Map.of(
                        "appVersion", appVersion,
                        "activeDevices", String.valueOf(state.connectedDeviceIds.size()),
                        "organisationName", "GP's on Vermont"
                )
        );

        log.infov("🟢 DEVICE_CONNECTED: device={0}, group={1}, activeDevices={2}",
                deviceId, groupId, state.connectedDeviceIds.size());

        // Replay active alerts to late joiner
        Uni<Void> replayAlerts = replayActiveAlerts(connection, state, deviceId);

        // Notify monitors about new online device
        DeviceMessage onlineNotification = new DeviceMessage(
                MessageType.DEVICE_CONNECTED,
                Map.of(
                        "deviceId", deviceId,
                        "status", DeviceStatus.ONLINE.name(),
                        "workplaceId", workplaceId
                )
        );

        return connection.sendText(jsonService.toJson(ack))
                .call(() -> replayAlerts)
                .call(() -> {
                    registry.broadcastToObservers(groupKey, onlineNotification);
                    return Uni.createFrom().voidItem();
                });
    }

    /**
     * Replay any active alerts to a newly connected device (late joiner)
     */
    private Uni<Void> replayActiveAlerts(WebSocketConnection connection, GroupState state, String deviceId) {
        if (state.activeAlarms.isEmpty()) {
            return Uni.createFrom().voidItem();
        }

        log.infov("📣 ACTIVE_ALERT_REPLAY: device={0} will receive {1} active alerts",
                deviceId, state.activeAlarms.size());

        Uni<Void> chain = Uni.createFrom().voidItem();

        for (DeviceMessage alert : state.activeAlarms.values()) {
            chain = chain.call(() -> connection.sendText(jsonService.toJson(alert)));
        }

        return chain;
    }
}
