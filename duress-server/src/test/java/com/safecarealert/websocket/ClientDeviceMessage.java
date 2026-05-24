package com.safecarealert.websocket;



import com.safecarealert.messages.MessageType;

import java.time.Instant;
import java.util.Map;

/**
 * Client-side representation of a message received from the server.
 * Mirrors the server's DeviceMessage structure but is safe for client use.
 */
public record ClientDeviceMessage(
        Instant timestamp,
        MessageType messageType,
        Map<String, Object> data,
        String correlationId
) {

    public ClientPayload payload() {
        return new ClientPayload(data);
    }
}
