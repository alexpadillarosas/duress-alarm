package com.safecarealert.alerts.registry;

import com.safecarealert.alerts.GroupKey;
import com.safecarealert.dashboard.MonitorSocket;
import com.safecarealert.dashboard.SubscriptionRequest;
import com.safecarealert.messages.DeviceMessage;
import com.safecarealert.utils.JsonService;
import com.safecarealert.websocket.MonitorContext;
import io.quarkus.websockets.next.OpenConnections;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * ObserverRegistry
 * ----------------
 * Handles broadcasting messages to dashboard/monitor observers.
 * <p>
 * Responsibilities:
 *   • Iterate over all active WebSocket connections
 *   • Identify those with a SubscriptionRequest in UserData
 *   • Check if the subscription scopes match the GroupKey
 *   • Push DeviceMessage updates to matching observers
 * <p>
 * This class contains ONLY observer logic.
 * GroupRegistry delegates to this class for dashboard pushes.
 */
@ApplicationScoped
public class ObserverRegistry {

    @Inject Logger log;
    @Inject JsonService jsonService;
    @Inject OpenConnections openConnections;

    /**
     * Broadcasts a message to all monitor/dashboard observers whose
     * subscription scopes match the given GroupKey.
     */
    public void broadcastToObservers(GroupKey key, DeviceMessage msg) {

        String jsonPayload = jsonService.toJson(msg);

        openConnections.stream()
                // Only connections with a stored subscription are observers
                .forEach(conn -> {

                    MonitorContext monitorContext = conn.userData().get(MonitorSocket.MONITOR_CONTEXT_TYPED_KEY);
                    if (monitorContext == null || monitorContext.activeSubscription() == null) {
                        return; // Not a monitor or not subscribed yet
                    }

                    SubscriptionRequest subscription = monitorContext.activeSubscription();

                    // Skip if subscription does not match this group
                    if (!subscription.matches(key)) {
                        return;
                    }

                    log.infov(
                            "📢 [OBSERVER_PUSH] Notifying monitor={0} about {1} in {2}/{3}",
                            conn.id(),
                            msg.messageType(),
                            key.tenantId(),
                            key.workplaceId()
                    );

                    conn.sendText(jsonPayload)
                            .subscribe().with(
                                    success -> log.tracev(
                                            "⬅️  OBSERVER_MESSAGE_SENT: connectionId={0}, type={1}",
                                            conn.id(),
                                            msg.messageType()
                                    ),
                                    failure -> log.errorv(
                                            "❌ OBSERVER_PUSH_FAILED: connectionId={0}, error={1}",
                                            conn.id(),
                                            failure.getMessage()
                                    )
                            );
                });
    }
}
