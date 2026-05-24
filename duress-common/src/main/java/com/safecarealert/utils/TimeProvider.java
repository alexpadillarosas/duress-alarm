package com.safecarealert.utils;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;

/**
 * TimeProvider
 * ------------
 * Provides timestamps in a testable, mockable way.
 *
 * Useful for:
 *   - alert timestamps
 *   - audit logs
 *   - correlation IDs
 */
@ApplicationScoped
public class TimeProvider {

    public Instant now() {
        return Instant.now();
    }
}
