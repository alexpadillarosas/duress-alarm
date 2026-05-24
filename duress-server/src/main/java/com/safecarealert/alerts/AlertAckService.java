package com.safecarealert.alerts;

import com.safecarealert.core.MessageStatus;
import com.safecarealert.messages.DeviceMessage;
import com.safecarealert.messages.MessageType;
import com.safecarealert.utils.JsonService;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Map;

/**
 * AlertAckService
 * ---------------
 * Centralised ACK builder/sender for alert lifecycle.
 *
 * MIGRATED FROM:
 *   AlertSocket.sendAck(...)
 *
 * The original method remains in AlertSocket for backward compatibility.
 */
@ApplicationScoped
public class AlertAckService {

    @Inject
    Logger log;

    @Inject
    JsonService jsonService;

    public Uni<Void> sendAck(WebSocketConnection connection,
                             MessageStatus status,
                             GroupState state,
                             String correlationId,
                             String deviceId) {

        String notified = String.join(", ",
                state.connectedDeviceIds.stream()
                        .filter(id -> !id.equals(deviceId))
                        .toList());

        DeviceMessage ack = new DeviceMessage(
                MessageType.MESSAGE_ACK,
                Map.of(
                        "status", status.name(),
                        "notifiedDevices", notified
                ),
                correlationId
        );

        // 📬 Restored expressive ACK log
        log.infov("📬 ACK_SENT: status={0}, correlationId={1}, fromDevice={2}, notifiedDevices=[{3}], data={4}",
                status.name(),
                correlationId,
                deviceId,
                notified,
                jsonService.toJson(ack));

        return connection.sendText(jsonService.toJson(ack));
    }
}
