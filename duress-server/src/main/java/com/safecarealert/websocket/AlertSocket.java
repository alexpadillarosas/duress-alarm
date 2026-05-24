package com.safecarealert.websocket;


import com.safecarealert.alerts.GroupKey;
import com.safecarealert.alerts.GroupState;
import com.safecarealert.alerts.registry.GroupRegistry;
import com.safecarealert.core.TerminationReason;
import com.safecarealert.dashboard.DeviceStatus;
import com.safecarealert.dashboard.MonitorSocket;
import com.safecarealert.messages.DeviceMessage;
import com.safecarealert.messages.MessageType;
import com.safecarealert.messaging.*;
import com.safecarealert.utils.JsonService;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.websockets.next.*;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Map;

/**
 * AlertSocket (v1)
 * ----------------
 * Updated to use:
 *   - ConnectionContext
 *   - new identity pipeline (CustomIdentityAugmentor)
 *   - ConnectionEvents
 *   - IncomingEnvelope + MessageRouter (Option 3)
 *   - Router handles CONNECT (AlertSocket no longer contains connect logic)
 */
@WebSocket(path = "/ws/v1/alerts")
public class AlertSocket {

    public static final UserData.TypedKey<String> DEVICE_ID     = UserData.TypedKey.forString("deviceId");
    public static final UserData.TypedKey<String> TENANT_ID     = UserData.TypedKey.forString("tenantId");
    public static final UserData.TypedKey<String> WORKPLACE_ID  = UserData.TypedKey.forString("workplaceId");
    public static final UserData.TypedKey<String> GROUP_ID      = UserData.TypedKey.forString("groupId");

    private static volatile boolean isShuttingDown = false;

    @Inject Logger log;
    @Inject JsonService jsonService;
    @Inject SecurityIdentity identity;
    @Inject WebSocketIdentityValidator validator;
    @Inject GroupRegistry registry;
    @Inject OpenConnections openConnections;
    @Inject MessageRouter messageRouter;
    @Inject ConnectionEvents connectionEvents;

    @Inject
    @ConfigProperty(name = "app.version")
    String appVersion;

    // -------------------------------------------------------------------------
    // ON OPEN
    // -------------------------------------------------------------------------

    @OnOpen
    @Blocking
    public Uni<Void> onOpen(WebSocketConnection connection) {

        if (isShuttingDown) {
            log.warnf("✋ Rejecting connection %s — server shutting down", connection.id());
            return connection.close(new CloseReason(
                    TerminationReason.SERVER_SHUTDOWN.getCode(),
                    TerminationReason.SERVER_SHUTDOWN.getDescription()
            ));
        }

        log.infov("🔌 [ALERT] WebSocket opened: id={0}, principal={1}, roles={2}",
                connection.id(), identity.getPrincipal().getName(), identity.getRoles());

        log.infov("🧩 Roles resolved for device={0}: {1}",
                identity.getPrincipal().getName(), identity.getRoles());

        return Uni.createFrom().voidItem();
    }

    // -------------------------------------------------------------------------
    // ON MESSAGE (Option 3: parse IncomingEnvelope → route everything)
    // -------------------------------------------------------------------------

    @OnTextMessage
    @Blocking
    public Uni<Void> onMessage(WebSocketConnection connection, String raw) {

        if (isShuttingDown) {
            log.debugv("⌛ Ignoring message {0} from {1} — server shutting down", raw, connection.id());
            return Uni.createFrom().voidItem();
        }

        IncomingEnvelope envelope;
        try {
            envelope = jsonService.fromJson(raw, IncomingEnvelope.class);
        } catch (Exception e) {
            log.warnf("❌ INVALID_JSON from %s: data= %s", connection.id(), raw);
            return sendErrorAndClose(connection, TerminationReason.INVALID_JSON, TerminationReason.INVALID_JSON.getDescription());
        }

        if (envelope == null || envelope.messageType() == null) {
            log.warnf("❌ INVALID_ENVELOPE from %s: data= %s", connection.id(), raw);
            return sendErrorAndClose( connection, TerminationReason.INVALID_JSON, "Missing messageType");
        }

        log.infov("➡️ MESSAGE_RECEIVED: type={0}, fromDevice={1}, connectionId={2}, data=📦 {3}",
                envelope.messageType(),
                connection.userData().get(DEVICE_ID),
                connection.id(),
                raw);

        try {
            return messageRouter.route(connection, envelope, identity);

        } catch (ProtocolException e) {
            log.warnf("%s PROTOCOL_ERROR from %s: %s",
                    e.reason(),
                    connection.id(),
                    e.getMessage());

            return sendErrorAndClose(connection, e.reason(), e.getMessage());

        } catch (Exception e) {
            log.errorf("💥 INTERNAL_ERROR while processing message from %s: %s",
                    connection.id(), e.getMessage());

            return sendErrorAndClose(connection,
                    TerminationReason.SYSTEM_FAULT,
                    "Internal server error");
        }
    }

    // -------------------------------------------------------------------------
    // ERROR HANDLING
    // -------------------------------------------------------------------------

    private Uni<Void> sendErrorAndClose(WebSocketConnection connection,
                                        TerminationReason reason,
                                        String details) {

        DeviceMessage error = new DeviceMessage(
                MessageType.SYSTEM_FAULT,
                Map.of(
                        "error", reason.name(),
                        "details", details
                )
        );

        log.errorf("💥 SYSTEM_FAULT for %s: %s", connection.id(), details);

        return connection.sendText(jsonService.toJson(error))
                .chain(() -> connection.close(new CloseReason(reason.getCode(), reason.getDescription())));
    }

    // -------------------------------------------------------------------------
    // ON CLOSE (unchanged)
    // -------------------------------------------------------------------------

    @OnClose
    public void onClose(WebSocketConnection connection, CloseReason reason) {

        ConnectionContext ctx = extractContext(connection);

        // Only process if it's a device
        if (!ctx.isDevice()) {
            return;
        }
        DeviceContext deviceCtx = (DeviceContext) ctx;
        connectionEvents.onDeviceDisconnected(connection, deviceCtx.deviceId());

        GroupKey groupKey = new GroupKey(ctx.tenantId(), deviceCtx.workplaceId(), deviceCtx.groupId());
        GroupState state = registry.getState(groupKey);

        if (state == null) return;

        boolean wasAlerting = state.activeAlarms.containsKey(deviceCtx.deviceId());

        state.connectedDeviceIds.remove(deviceCtx.deviceId());
        state.activeAlarms.remove(deviceCtx.deviceId());

        registry.unregisterDevice(groupKey, connection);

        if (wasAlerting) {
            DeviceMessage stop = new DeviceMessage(
                    MessageType.ALERT_STOP,
                    Map.of(
                            "alertOwner", deviceCtx.deviceId(),
                            "timestamp", Instant.now().toString()
                    )
            );

            log.infov("⛔ ALERT_STOP triggered due to owner disconnect: {0}", deviceCtx.deviceId());

            registry.broadcast(groupKey, stop, connection);
            registry.broadcastToObservers(groupKey, stop);
        }

        DeviceMessage offline = new DeviceMessage(
                MessageType.DEVICE_DISCONNECTED,
                Map.of(
                        "deviceId", deviceCtx.deviceId(),
                        "status", DeviceStatus.OFFLINE.name(),
                        "reason", TerminationReason.fromCode(reason.getCode(), wasAlerting).name(),
                        "lastSeen", Instant.now().toString()
                )
        );

        log.infov("🔴 Device disconnected: {0} ({1})", deviceCtx.deviceId(), TerminationReason.fromCode(reason.getCode(), wasAlerting).name());

        registry.broadcastToObservers(groupKey, offline);
    }

    /**
     * Helper to extract context (used in onClose)
     */
    private ConnectionContext extractContext(WebSocketConnection connection) {
        // This is the same helper as in MessageRouter
        MonitorContext monitorCtx = connection.userData().get(MonitorSocket.MONITOR_CONTEXT_TYPED_KEY);
        if (monitorCtx != null) {
            return monitorCtx;
        }

        var ud = connection.userData();
        return new DeviceContext(
                ud.get(TENANT_ID),
                ud.get(WORKPLACE_ID),
                ud.get(GROUP_ID),
                ud.get(DEVICE_ID)
        );
    }

    // -------------------------------------------------------------------------
    // ON ERROR (unchanged)
    // -------------------------------------------------------------------------

    @OnError
    public Uni<Void> onError(WebSocketConnection connection, Throwable error) {

        String clientId = connection != null ? connection.userData().get(DEVICE_ID) : "unknown";

        log.errorf(error, "💥 WebSocket error for device {0}", clientId);

        TerminationReason reason = (error instanceof JsonProcessingException)
                ? TerminationReason.INVALID_JSON
                : TerminationReason.INTERNAL_SERVER_ERROR;

        return connection.close(new CloseReason(reason.getCode(), reason.getDescription()));
    }

    // -------------------------------------------------------------------------
    // ON PONG (unchanged)
    // -------------------------------------------------------------------------

    public void onPong(WebSocketConnection connection, Buffer data) {
        log.debugf("💓 Heartbeat received from device %s", connection.userData().get(DEVICE_ID));
    }

    // -------------------------------------------------------------------------
    // SHUTDOWN (unchanged)
    // -------------------------------------------------------------------------

    public void prepareForShutdown() {
        isShuttingDown = true;
        log.info("📢 AlertSocket: preparing for shutdown (no longer accepting messages)...");
    }
}
