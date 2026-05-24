package com.safecarealert.alerts;


import com.safecarealert.alerts.registry.GroupRegistry;
import com.safecarealert.core.MessageStatus;
import com.safecarealert.messages.AlertStopMessage;
import com.safecarealert.messages.DeviceMessage;
import com.safecarealert.messages.MessageType;
import com.safecarealert.utils.JsonService;
import io.quarkus.logging.Log;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;

/**
 * AlertStopHandler
 * ----------------
 * Handles ALERT_STOP messages (typed inbound).
 */
@ApplicationScoped
public class AlertStopHandler {

    @Inject
    GroupRegistry registry;

    @Inject
    JsonService jsonService;

    @Inject
    AlertAckService ackService;

    public Uni<Void> handle(WebSocketConnection connection,
                            GroupKey groupKey,
                            String deviceId,
                            AlertStopMessage msg) {

        GroupState state = registry.getState(groupKey);

        String target = msg.alertOwner();
        DeviceMessage original = state.activeAlarms.remove(target);

        if (original == null) {
            Log.debugf("⚠️ ALERT already stopped for {0}", target);
            return ackService.sendAck(connection,
                    MessageStatus.ALERT_ALREADY_STOPPED,
                    state,
                    msg.correlationId(),
                    deviceId);
        }

        DeviceMessage stopMsg = new DeviceMessage(
                MessageType.ALERT_STOP,
                Map.of(
                        "alertOwner", target,
                        "stoppedBy", deviceId
                ),
                msg.correlationId()
        );

        Log.infov("📣 ALERT_STOP_BROADCAST: owner={0}, stoppedBy={1}, correlationId={2}, data={3}",
                target, deviceId, msg.correlationId(), jsonService.toJson(stopMsg));

        registry.broadcast(groupKey, stopMsg, connection);
        registry.broadcastToObservers(groupKey, stopMsg);

        return ackService.sendAck(connection,
                MessageStatus.STOP_COMPLETE,
                state,
                msg.correlationId(),
                deviceId);
    }
}
