package com.safecarealert.messaging;

import io.vertx.core.json.JsonObject;

/**
 * IncomingEnvelope
 * ----------------
 * Generic inbound wrapper used by AlertSocket and MessageRouter.
 * Carries messageType, data, and correlationId.
 */
public record IncomingEnvelope(
        String messageType,
        JsonObject data,
        String correlationId
) {}
