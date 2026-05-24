package com.safecarealert.messages;

/**
 * AlertStartMessage
 * -----------------
 * Typed inbound DTO for ALERT_START messages.
 */
public record AlertStartMessage(
        String location,
        String person,
        String alertOwner,
        String message,
        String correlationId
) {}
