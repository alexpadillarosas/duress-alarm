package com.safecarealert.rubbish;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * IdentityParser
 * --------------
 * Centralised parsing of identity-related fields from:
 *   - JWT claims
 *   - CONNECT payloads
 *   - headers
 *
 * This removes parsing logic from AlertSocket, MonitorSocket,
 * WebSocketIdentityValidator, and CustomIdentityAugmentor.
 */
@ApplicationScoped
public class IdentityParser {

    @Inject
    Logger log;

    public String parseSubject(Object raw) {
        if (raw == null) return null;
        String value = raw.toString().trim();
        log.debugf("🔍 Parsed subjectUUID: %s", value);
        return value;
    }

    public String parseTenant(Object raw) {
        if (raw == null) return null;
        String value = raw.toString().trim();
        log.debugf("🔍 Parsed tenantUUID: %s", value);
        return value;
    }

    public String parseWorkplace(Object raw) {
        if (raw == null) return null;
        String value = raw.toString().trim();
        log.debugf("🔍 Parsed workplaceUUID: %s", value);
        return value;
    }

    public String parseGroup(Object raw) {
        if (raw == null) return null;
        String value = raw.toString().trim();
        log.debugf("🔍 Parsed groupUUID: %s", value);
        return value;
    }
}
