package com.safecarealert.messaging;

import java.util.Map;

/** * RawMessage
 * ---------- *
 * Direct JSON mapping of inbound messages.
 * No enums, no validation, just raw fields.
 */
public class RawMessage {
    public String messageType;
    public Map<String, Object> data;
    public String correlationId;
}
