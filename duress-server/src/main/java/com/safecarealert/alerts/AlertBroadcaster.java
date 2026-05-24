package com.safecarealert.alerts;

import com.safecarealert.alerts.registry.GroupRegistry;
import com.safecarealert.messages.DeviceMessage;
import io.quarkus.logging.Log;
import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * AlertBroadcaster
 * ----------------
 * High-level semantic broadcast helper for alert lifecycle events.
 *
 * NOTE:
 *   This does NOT replace GroupRegistry.broadcast(...) because that method
 *   handles connection cleanup and low-level delivery.
 *
 *   Instead, this class provides expressive, semantic broadcast helpers
 *   used by AlertStartHandler and AlertStopHandler.
 */
@ApplicationScoped
public class AlertBroadcaster {

    @Inject
    GroupRegistry registry;

    /**
     * Broadcasts an ALERT_START message to devices + observers.
     */
    public void broadcastAlertStart(GroupKey key,
                                    DeviceMessage msg,
                                    WebSocketConnection origin) {

        Log.infov("📣 [BROADCAST] ALERT_START → group={0}, correlationId={1}",
                key, msg.correlationId());

        registry.broadcast(key, msg, origin);
        registry.broadcastToObservers(key, msg);
    }

    /**
     * Broadcasts an ALERT_STOP message to devices + observers.
     */
    public void broadcastAlertStop(GroupKey key,
                                   DeviceMessage msg,
                                   WebSocketConnection origin) {

        Log.infov("📣 [BROADCAST] ALERT_STOP → group={0}, correlationId={1}",
                key, msg.correlationId());

        registry.broadcast(key, msg, origin);
        registry.broadcastToObservers(key, msg);
    }
}
