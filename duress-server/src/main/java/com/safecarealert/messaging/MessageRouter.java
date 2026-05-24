package com.safecarealert.messaging;

import com.safecarealert.alerts.AlertStartHandler;
import com.safecarealert.alerts.AlertStopHandler;
import com.safecarealert.alerts.GroupKey;
import com.safecarealert.core.ConnectMessage;
import com.safecarealert.core.TerminationReason;
import com.safecarealert.dashboard.MonitorSocket;
import com.safecarealert.messages.AlertStartMessage;
import com.safecarealert.messages.AlertStopMessage;
import com.safecarealert.messages.MessageType;
import com.safecarealert.utils.JsonService;
import com.safecarealert.websocket.AlertSocket;
import com.safecarealert.websocket.ConnectionContext;
import com.safecarealert.websocket.DeviceContext;
import com.safecarealert.websocket.MonitorContext;
import com.safecarealert.websocket.connect.ConnectHandler;
import io.quarkus.logging.Log;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * MessageRouter
 * -------------
 * Routes typed inbound messages (via IncomingEnvelope) to feature handlers.
 * <p>
 * Responsibilities:
 *   - Validate message sequence (CONNECT must be first)
 *   - Convert envelope.messageType (String) → MessageType enum safely
 *   - Deserialize envelope.data into typed DTOs
 *   - Delegate to ConnectHandler, AlertStartHandler, AlertStopHandler
 *   - Throw ProtocolException for unknown message types
 */
@ApplicationScoped
public class MessageRouter {

    @Inject
    ConnectHandler connectHandler;

    @Inject
    AlertStartHandler startHandler;

    @Inject
    AlertStopHandler stopHandler;

    @Inject
    JsonService jsonService;

    public Uni<Void> route(WebSocketConnection connection,
                           IncomingEnvelope envelope,
                           SecurityIdentity identity) {

        // ---------------------------------------------------------------------
        // Parse messageType safely (String → enum)
        // ---------------------------------------------------------------------
        MessageType type = MessageType.safeParse(envelope.messageType());

        if (type == null) {
            throw new ProtocolException(
                    TerminationReason.UNKNOWN_MESSAGE_TYPE,
                    TerminationReason.UNKNOWN_MESSAGE_TYPE.getDescription()
            );
        }

        // CONNECT can come from both
        if (type == MessageType.CONNECT) {
            ConnectMessage msg = jsonService.fromJson(envelope.data().encode(), ConnectMessage.class);
            return connectHandler.handle(connection, msg, identity);
        }

        ConnectionContext ctx = extractContext(connection);
        Log.infov("🧭 ROUTING: {0} from {1} ({2})", type, ctx.clientId(), ctx.clientType());
        // After CONNECT, route based on client type
        if (ctx.isDevice()) {
            DeviceContext deviceCtx = (DeviceContext) ctx;
            GroupKey groupKey = new GroupKey(deviceCtx.tenantId(), deviceCtx.workplaceId(), deviceCtx.groupId());

            return switch (type) {
                case ALERT_START -> {
                    AlertStartMessage msg = jsonService.fromJson(envelope.data().encode(), AlertStartMessage.class);
                    yield startHandler.handle(connection, groupKey, deviceCtx.deviceId(), msg);
                }
                case ALERT_STOP -> {
                    AlertStopMessage msg = jsonService.fromJson(envelope.data().encode(), AlertStopMessage.class);
                    yield stopHandler.handle(connection, groupKey, deviceCtx.deviceId(), msg);
                }
                default -> throw new ProtocolException(TerminationReason.UNKNOWN_MESSAGE_TYPE, "Unsupported message for device");
            };
        }
        else if (ctx.isMonitor()) {
            // Monitors should only send subscription messages (handled in MonitorSocket)
            Log.warnv("⚠️ Monitor tried to send device message type: {0}", type);
            throw new ProtocolException(TerminationReason.FORBIDDEN, "Monitors cannot send alert commands");
        }

        throw new ProtocolException(TerminationReason.UNKNOWN_MESSAGE_TYPE, "Unknown client type");
    }

    /**
     * Helper to extract context from connection userData
     */
    private ConnectionContext extractContext(WebSocketConnection connection) {
        // Check for Monitor context first
        MonitorContext monitorCtx = connection.userData().get(MonitorSocket.MONITOR_CONTEXT_TYPED_KEY);
        if (monitorCtx != null) {
            return monitorCtx;
        }

        // Otherwise assume Device context (legacy style)
        var ud = connection.userData();
        String tenantId = ud.get(AlertSocket.TENANT_ID);
        String workplaceId = ud.get(AlertSocket.WORKPLACE_ID);
        String groupId = ud.get(AlertSocket.GROUP_ID);
        String deviceId = ud.get(AlertSocket.DEVICE_ID);

        if (deviceId != null && groupId != null) {
            return new DeviceContext(tenantId, workplaceId, groupId, deviceId);
        }

        throw new ProtocolException(TerminationReason.INVALID_MESSAGE_SEQUENCE, "No valid context found. Did you sent CONNECT first?");
    }
}
