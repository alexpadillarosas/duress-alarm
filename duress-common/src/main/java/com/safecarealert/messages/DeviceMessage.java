package com.safecarealert.messages;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/***
 * Record used to send data back and forth between server and clients
 * @param timestamp     The timestamp assigned by the server
 * @param messageType
 * @param correlationId When the server broadcasts an alert, it assigns a correlationId. When the client receives it, it sends back a message of type MESSAGE_ACK with that same ID. This allows your GroupRegistry to cross-reference exactly which device has seen which alert.
 * @param data          Map containing the data
 */
public record DeviceMessage(
        Instant timestamp,
        MessageType messageType,
        Map<String, Object> data,
        String correlationId
) {

    /**
     * Compact constructor for validation and defaults.
     */
    public DeviceMessage{
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    /**
     * Constructor for basic messages that don't require tracking.
     */
    public DeviceMessage(MessageType messageType, Map<String, Object> map) {
        this(Instant.now(), messageType, map, null);
    }

    /**
     * Constructor for basic messages that don't require tracking.
     */
    public DeviceMessage(MessageType messageType, Map<String, Object> map, String correlationId) {
        this(Instant.now(), messageType, map, correlationId);
    }

    public static String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Creates a copy of this message but with a new correlationId.
     * Useful for replying to a message or tagging it for tracking.
     */
    public DeviceMessage withCorrelationId() {
        // We reuse the existing timestamp, type, and data
        return new DeviceMessage(this.timestamp, this.messageType, this.data(), UUID.randomUUID().toString());
    }

//    public Payload payload() {
//        return new Payload(data);
//    }

//    /**
//     * Factory: Use this for basic messages (Generates a new UUID).
//     */
//    public static DeviceMessage withAutoCorrelationId(MessageType type, Map<String, String> data) {
//        return new DeviceMessage(null, type, UUID.randomUUID().toString(), data);
//    }
//
//    /**
//     * Factory: Use this for ACKs (Reuses an existing ID).
//     */
//    public static DeviceMessage withCustomCorrelationId(MessageType type, String correlationId, Map<String, String> data) {
//        return new DeviceMessage(null, type, correlationId, data);
//    }

    /**
     * Constructor for critical messages that require an ACK.
     * Generates a unique correlationId automatically.
     */
//    public static DeviceMessage createCritical(MessageType messageType, Map<String, String> data) {
//        return new DeviceMessage(Instant.now(), messageType, UUID.randomUUID().toString(), data);
//    }


}


/*
    Example of data (again: Server determines who sent it, not the data)
 {
  "type": "ALERT_START",
  "data": {
    "room": "ER-3",
    "person": "John Doe"
  }
 }
 */