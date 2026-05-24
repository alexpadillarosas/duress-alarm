package com.safecarealert.messages;

/**
 * AlertStopMessage
 * ----------------
 * Typed inbound DTO for ALERT_STOP messages.
 */
public record AlertStopMessage(
        String alertOwner,
        String correlationId
) {}
