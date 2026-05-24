package com.safecarealert.messages;

public enum MessageType {

    // --- Alert Lifecycle ---
    ALERT_START,        // Device initiated an emergency
    ALERT_STOP,         // Device requested to silence an emergency

    // --- System & Registry Events ---
    DEVICE_CONNECTED,    // Server handshake confirmation (Hydration)
    DEVICE_DISCONNECTED, // Peer notification that someone left
    SYSTEM_FAULT,        // Notification of a crash or unlicensed state
    SERVER_RESTART,      // Graceful shutdown notification

    // --- Request/Response Patterns ---
    MESSAGE_ACK,         // Generic response to any command

    // --- Information Queries ---
    LIST_DEVICES_IN_GROUP, // Client requesting peer list
    QUERY_SYSTEM_INFO,      // Client requesting version/server stats

    CONNECT;

    /**
     * This prevents enum exceptions from being misinterpreted as INVALID_JSON
     * @param raw  The String enum value that must match the Enum Constant
     * @return The MessageType if found in as Enum Constant
     */
    public static MessageType safeParse(String raw){
        if (raw == null) return null;
        try{
            return MessageType.valueOf(raw);
        } catch (Exception e){
            return null;
        }
    }

}
