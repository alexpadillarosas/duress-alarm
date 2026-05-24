package com.safecarealert.dashboard;

import com.safecarealert.alerts.registry.GroupRegistry;
import com.safecarealert.core.ConnectMessage;
import com.safecarealert.core.ConnectValidation;
import com.safecarealert.core.TerminationReason;
import com.safecarealert.identity.Claim;
import com.safecarealert.messages.DeviceMessage;
import com.safecarealert.messages.MessageType;
import com.safecarealert.utils.JsonService;
import com.safecarealert.websocket.MonitorContext;
import com.safecarealert.websocket.WebSocketIdentityValidator;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.websockets.next.*;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * MonitorSocket (v1)
 * ------------------
 * WebSocket endpoint for MONITOR, SYSTEM_ADMIN, and SUPPORT users.
 * <p>
 * Protocol:
 *   1. Client connects to /ws/v1/monitor
 *   2. First message MUST be a CONNECT payload
 *   3. After CONNECT, client sends SubscriptionRequest messages
 *   4. Server validates subscription scopes against identity
 *   5. Server hydrates monitor with current active alarms
 * <p>
 * Identity:
 *   - JWT determines tenant/workplace/group access
 *   - CONNECT payload provides allowedWorkplaces for MONITOR role
 * <p>
 * Notes:
 *   - This socket does NOT use the device protocol (alerts)
 *   - This socket does NOT use IncomingEnvelope or MessageRouter
 */
@WebSocket(path = "/ws/v1/monitor")
public class MonitorSocket {

    /** Stores the active subscription for this connection */
    public static final UserData.TypedKey<MonitorContext> MONITOR_CONTEXT_TYPED_KEY =
            new UserData.TypedKey<>("monitorContext");

    @Inject Logger log;
    @Inject JsonService jsonService;
    @Inject GroupRegistry registry;
    @Inject SecurityIdentity identity;
    @Inject WebSocketIdentityValidator validator;

    // -------------------------------------------------------------------------
    // ON OPEN
    // -------------------------------------------------------------------------

    @OnOpen
    public void onOpen(WebSocketConnection connection) {
        log.infov("🖥️ [MONITOR] Connection opened: id={0}, principal={1}, roles={2}",
                connection.id(),
                identity.getPrincipal().getName(),
                identity.getRoles());
    }

    // -------------------------------------------------------------------------
    // ON MESSAGE
    // -------------------------------------------------------------------------

    @OnTextMessage
    public Uni<Void> onMessage(WebSocketConnection connection, String raw) {

        // 1) Try CONNECT first
        try {
            ConnectMessage connect = jsonService.fromJson(raw, ConnectMessage.class);

            if (ConnectValidation.isMonitorConnect(connect)) {
                return handleConnect(connection, connect);
            }
        } catch (Exception ignored) {
            // Not a CONNECT payload → fall through
        }

        // 2) Otherwise treat it as SubscriptionRequest
        SubscriptionRequest request;
        try {
            request = jsonService.fromJson(raw, SubscriptionRequest.class);
        } catch (Exception e) {
            log.errorv("❌ [MONITOR] Invalid subscription JSON: {0}", e.getMessage());
            return Uni.createFrom().voidItem();
        }

        return handleSubscription(connection, request);
    }

    // -------------------------------------------------------------------------
    // CONNECT HANDSHAKE
    // -------------------------------------------------------------------------

    private Uni<Void> handleConnect(WebSocketConnection connection, ConnectMessage msg) {

        log.infov("🖥️ [MONITOR] CONNECT received: allowedWorkplaces={0}, subject={1}",
                msg.allowedWorkplaces(),
                msg.subjectId());

        // CONNECT for monitors is lightweight:
        // - Identity is already validated by JWT
        // - Full authorization happens during subscription
        return Uni.createFrom().voidItem();
    }

    // -------------------------------------------------------------------------
    // SUBSCRIPTION HANDLING
    // -------------------------------------------------------------------------

    private Uni<Void> handleSubscription(WebSocketConnection connection, SubscriptionRequest request) {

        if (request.action() == SubscriptionAction.SUBSCRIBE) {

            // Validate subscription scopes against identity
            if (!validator.validateMonitorContext(identity, request)) {

                log.errorv("🚫 [MONITOR] Forbidden subscription attempt. user={0}, scopes={1}",
                        identity.getPrincipal().getName(),
                        request.monitorScopes());

                return connection.close(new CloseReason(
                        TerminationReason.FORBIDDEN.getCode(),
                        TerminationReason.FORBIDDEN.getDescription()
                ));
            }

            String tenantId = identity.getAttribute(Claim.TENANT_UUID.name());
            String monitorId = identity.getAttribute(Claim.SUBJECT_UUID.name());
            MonitorContext monitorContext = new MonitorContext(tenantId, monitorId, request);

            // Store subscription
            connection.userData().put(MONITOR_CONTEXT_TYPED_KEY, monitorContext);

            log.infov("📡 [MONITOR] SUBSCRIBED: id={0}, scopes={1}",
                    connection.id(),
                    request.monitorScopes());

            // Hydrate monitor with current active alarms
            return pushCurrentState(connection, request);
        }

        if (request.action() == SubscriptionAction.UNSUBSCRIBE) {
            connection.userData().remove(MONITOR_CONTEXT_TYPED_KEY);

            log.infov("🛑 [MONITOR] UNSUBSCRIBED: id={0}", connection.id());
            return Uni.createFrom().voidItem();
        }

        log.warnv("⚠️ [MONITOR] Unknown subscription action: {0}", request.action());
        return Uni.createFrom().voidItem();
    }

    // -------------------------------------------------------------------------
    // HYDRATION
    // -------------------------------------------------------------------------

    private Uni<Void> pushCurrentState(WebSocketConnection connection, SubscriptionRequest request) {

        Map<String, DeviceMessage> matches = new HashMap<>();

        registry.getGroupStates().forEach((groupKey, state) -> {
            if (request.matches(groupKey)) {
                matches.putAll(state.activeAlarms);
            }
        });

        if (matches.isEmpty()) {
            return Uni.createFrom().voidItem();
        }

        DeviceMessage hydration = new DeviceMessage(
                MessageType.DEVICE_CONNECTED,
                Map.of("activeAlarms", jsonService.toJson(matches))
        );

        log.infov("💧 [MONITOR] Hydrating monitor: id={0}, alarms={1}",
                connection.id(),
                matches.size());

        return connection.sendText(jsonService.toJson(hydration));
    }

    // -------------------------------------------------------------------------
    // ON CLOSE
    // -------------------------------------------------------------------------

    @OnClose
    public void onClose(WebSocketConnection connection) {
        log.infov("🖥️ [MONITOR] Disconnected: id={0}", connection.id());
    }
}
