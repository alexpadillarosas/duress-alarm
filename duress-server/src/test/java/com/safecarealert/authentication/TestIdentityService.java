package com.safecarealert.authentication;

import com.safecarealert.identity.IdentityRecord;
import com.safecarealert.identity.IdentityService;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test-only IdentityService implementation.
 * <p>
 * This service provides an in-memory registry of IdentityRecord instances.
 * It is activated in test environments via @Alternative @Priority(1).
 * <p>
 * IMPORTANT:
 *  - Application code MUST NOT depend on this service.
 *  - Only CustomIdentityAugmentor and SubjectFactory should call lookup().
 *  - Tests may register arbitrary identities to simulate any scenario.
 * <p>
 * This behaves exactly like a DB-backed identity service would,
 * but without persistence or external dependencies.
 */
@Alternative
@Priority(1)
@ApplicationScoped
public class TestIdentityService implements IdentityService {

    /**
     * Internal identity registry keyed by subjectUUID.
     * tenantHint is intentionally ignored — the registry is authoritative.
     */
    private final Map<String, IdentityRecord> registry = new ConcurrentHashMap<>();

    @Override
    public IdentityRecord lookup(String subjectUUID, String tenantHint) {
        return registry.get(subjectUUID);
    }

    @Override
    public void register(IdentityRecord record) {
        registry.put(record.subjectUUID(), record);
    }

    @Override
    public void clear() {
        registry.clear();
    }
}
