package com.safecarealert.core;

public enum TerminationReason {

    // --- Normal Lifecycle (1000–1011) ---
    CLEAN_EXIT(1000, "🟢", "Connection closed normally"),
    ABNORMAL_DISCONNECT(1006, "⚠️", "Connection lost unexpectedly"),

    // --- Envelope / Transport Errors (4000–4009) ---
    INVALID_JSON(4000, "🧩", "Malformed JSON or invalid envelope structure"),
    MISSING_MESSAGE_TYPE(4001, "⚠️", "Envelope missing required field: messageType"),

    // --- Protocol Errors (4010–4020) ---
    UNKNOWN_MESSAGE_TYPE(4010, "❓", "Message type is not recognised by the system"),
    INVALID_MESSAGE_SEQUENCE(4011, "🔄", "Message not allowed before CONNECT handshake"),
    INVALID_MESSAGE_PAYLOAD(4012, "📦", "Payload does not match expected message format"),
    INVALID_CORRELATION_ID(4013, "🔗", "Invalid or unknown correlationId"),

    // --- Authorization / Identity Errors (4030–4040) ---
    UNAUTHORIZED(4030, "🔐", "Device is not authorized to perform this operation"),
    FORBIDDEN(4031, "⛔", "Operation is forbidden for this device"),

    // --- Alert Lifecycle Errors (4100–4110) ---
    DEVICE_CRASHED_DURING_ALERT(4100, "🚨", "Device disconnected while alert was active"),

    // --- Server Errors (5000+) ---
    INTERNAL_SERVER_ERROR(5000, "💥", "An unexpected server error occurred"),
    SYSTEM_FAULT(5001, "🛑", "A protocol error occurred"),
    SERVER_SHUTDOWN(5002, "🛠️", "Server is shutting down");

    private final Integer code;
    private final String emoji;
    private final String description;

    TerminationReason(Integer code, String emoji, String description) {
        this.code = code;
        this.emoji = emoji;
        this.description = description;
    }

    public static TerminationReason fromCode(Integer code, boolean wasActiveAlertOwner) {
        if (wasActiveAlertOwner && code != 1000) {
            return DEVICE_CRASHED_DURING_ALERT;
        }
        if (code == 1000 || code == 1001) {
            return CLEAN_EXIT;
        }
        return ABNORMAL_DISCONNECT;
    }

    public Integer getCode() { return code; }
    public String getEmoji() { return emoji; }
    public String getDescription() { return description; }
}



//package com.safetycarealert.messaging;
//
//public enum TerminationReason {
//    /** The user manually closed the application or logged out. */
//
//    // --- Standard WebSocket Codes (1000-1011) ---
//    CLEAN_EXIT(1000, " 🟢 ", "User logged out or closed app"),
//    ABNORMAL_DISCONNECT(1006, " ⚠️ ", "Network lost or heartbeat timeout"),
//
//    // --- Client-Side Policy Violations (4000-4100) ---
//    DEVICE_ID_REQUIRED(4001, " 🆔 ", "Device ID parameter is missing"),
//    WORKPLACE_ID_REQUIRED(4002, " 🏢 ", "Workplace ID parameter is missing"),
//    TENANT_ID_REQUIRED(4003, " 🏢 ", "Tenant ID parameter is missing"),
//    GROUP_ID_REQUIRED(4004, " 👥 ", "Group ID parameter is missing"),
//    DEVICE_UNLICENSED(4005, " 🚫 ", "Device is not authorized or licensed"),
//    VIOLATED_POLICY(4006, " 🔒 ", "Request parameters failed validation"),
//    INVALID_JSON(4007, " {x} ", "JSON data is malformed or structure is invalid"),
////    INVALID_MESSAGE(4008," ⛓️‍💥 ", "The message format is not supported"),
//    UNKNOWN_MESSAGE_TYPE(4009, " ❓ ", "Message type is not recognised by the system"),
//    MISSING_AUTHORISATION_HEADER(4010, " ❌ ","Missing Authorisation header"),
//    IDENTITY_DOES_NOT_MATCH_REQUEST_CONTEXT(4011, " ❌ ", "Identity does not match requested context"),
//    MONITOR_NOT_ALLOWED_TO_SUBSCRIBE_TO_REQUESTED_SCOPES(4012, " ❌ ", "Monitor not allowed to subscribe to requested scopes"),
//    INVALID_MESSAGE_PAYLOAD(4013, "", "Invalid Message Payload"),
//    INVALID_MESSAGE_SEQUENCE(4014,"", "Device sent message before CONNECT handshake"),
//    MISSING_MESSAGE_TYPE(4015, "⚠️", "Missing required field: messageType"),
//    INVALID_FIELD_TYPE(4016, "⚠️", "Field type mismatch"),
//    INVALID_CORRELATION_ID(4017, "🔗", "Invalid correlationId"),
//    // --- Critical Operational States (4200+) ---
//    DEVICE_CRASHED_DURING_ALERT(4201, " 🚨 ", "Device lost connection while alerting"),
//
//    // --- Server-Side States (5000+) ---
//    INTERNAL_SERVER_ERROR(5000, " 💥 ", "An unexpected server error occurred"),
//    SYSTEM_FAULT(5001,"","System fault"),
//    SERVER_SHUTDOWN(5002, " 🛠️ ", "Server maintenance in progress");
//
//
////✓🔴🟠🟡🟢🔵🟣
//    private final Integer code;
//    private final String emoji;
//    private final String description;
//
//    TerminationReason(Integer code, String emoji, String description) {
//        this.code = code;
//        this.emoji = emoji;
//        this.description = description;
//    }
//
//    public static TerminationReason fromCode(Integer code, boolean wasActiveAlertOwner) {
//        if (wasActiveAlertOwner && code != 1000) {
//            return DEVICE_CRASHED_DURING_ALERT;
//        }
//        if (code == 1000 || code == 1001) {
//            return CLEAN_EXIT;
//        }
//        return ABNORMAL_DISCONNECT;
//    }
//
//    public String getDescription() {
//        return description;
//    }
//
//    public String getEmoji() {
//        return emoji;
//    }
//
//    public Integer getCode() {
//        return code;
//    }
//}
