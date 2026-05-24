package com.safecarealert.alerts;

import com.safecarealert.alerts.registry.GroupRegistry;
import com.safecarealert.core.MessageStatus;
import com.safecarealert.messages.AlertStartMessage;
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
 * AlertStartHandler
 * -----------------
 * Handles ALERT_START messages (typed inbound).
 */
@ApplicationScoped
public class AlertStartHandler {

    @Inject
    GroupRegistry registry;

    @Inject
    JsonService jsonService;

    @Inject
    AlertAckService ackService;

    public Uni<Void> handle(WebSocketConnection connection,
                            GroupKey groupKey,
                            String deviceId,
                            AlertStartMessage msg) {

        GroupState state = registry.getState(groupKey);

        if (state.activeAlarms.containsKey(deviceId)) {
            Log.debugv("⚠️ ALERT already active for {0}", deviceId);
            return ackService.sendAck(connection,
                    MessageStatus.ALERT_ALREADY_STARTED,
                    state,
                    msg.correlationId(),
                    deviceId);
        }

        String correlationId = msg.correlationId();
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = DeviceMessage.generateCorrelationId();
        }

        Map<String, Object> payload = Map.of(
                "location", msg.location(),
                "person", msg.person(),
                "alertOwner", msg.alertOwner(),
                "message", msg.message()
        );

        DeviceMessage alertMsg = new DeviceMessage(
                MessageType.ALERT_START,
                payload,
                correlationId
        );


        state.activeAlarms.put(deviceId, alertMsg);

        Log.infov("📣 ALERT_START_BROADCAST: owner={0}, correlationId={1}, data={2}",
                deviceId, correlationId, jsonService.toJson(alertMsg));

        registry.broadcast(groupKey, alertMsg, connection);
        registry.broadcastToObservers(groupKey, alertMsg);

        return ackService.sendAck(connection,
                MessageStatus.BROADCAST_COMPLETE,
                state,
                correlationId,
                deviceId);
    }
}
