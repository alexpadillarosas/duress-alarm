package com.safecarealert.websocket;

import com.safecarealert.messages.MessageType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class DeviceMessageBuilder {

    private MessageType messageType;
    private String correlationId;
    private final Map<String, Object> data = new LinkedHashMap<>();

    private DeviceMessageBuilder() {}

    public static DeviceMessageBuilder create(MessageType type) {
        DeviceMessageBuilder b = new DeviceMessageBuilder();
        b.messageType = type;
        return b;
    }

    public DeviceMessageBuilder put(String key, Object value) {
        data.put(key, value);
        return this;
    }

    public DeviceMessageBuilder putAll(Map<String, ?> map) {
        data.putAll(map);
        return this;
    }

    public DeviceMessageBuilder withCorrelationId(String id) {
        this.correlationId = id;
        return this;
    }

    public DeviceMessageBuilder withAutoCorrelationId() {
        this.correlationId = UUID.randomUUID().toString();
        return this;
    }

    /**
     * Builds a JSON-ready envelope matching the server protocol.
     */
    public Map<String, Object> build() {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("messageType", messageType.name());
        msg.put("data", data);
        if (correlationId != null) {
            msg.put("correlationId", correlationId);
        }
        return msg;
    }
}

