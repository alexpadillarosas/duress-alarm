package com.safecarealert.websocket;

import com.safecarealert.messages.MessageType;
import com.safecarealert.utils.JsonService;

import java.time.Instant;
import java.util.Map;

public final class ClientMessageParser {

    private final JsonService json;

    public ClientMessageParser(JsonService json) {
        this.json = json;
    }

    @SuppressWarnings("unchecked")
    public ClientDeviceMessage parse(String raw) {
        Map<String, Object> map = json.fromJson(raw, Map.class);

        Instant timestamp = Instant.parse((String) map.get("timestamp"));
        MessageType type = MessageType.valueOf((String) map.get("messageType"));
        Map<String, Object> data = (Map<String, Object>) map.get("data");
        String correlationId = (String) map.get("correlationId");

        return new ClientDeviceMessage(timestamp, type, data, correlationId);
    }
}
