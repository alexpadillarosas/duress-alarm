package com.safecarealert.identity;

import java.util.Set;

/**
 * Internal identity lookup model used exclusively by the IdentityService
 * and CustomIdentityAugmentor.
 *
 * This interface represents the server‑truth identity record retrieved
 * from the identity registry (in‑memory or DB‑backed).
 *
 * Application code MUST NOT use IdentityRecord directly.
 * Instead, the augmentor converts IdentityRecord → Subject,
 * which is the unified identity model used throughout the system.
 */
public sealed interface IdentityRecord permits DeviceIdentity, MonitorIdentity, AdminIdentity, SupportIdentity {

    /**
     * Unique identifier for the identity (device UUID, user UUID, etc.)
     */
    String subjectUUID();

    /**
     * Tenant the identity belongs to.
     * May be null for system administrators.
     */
    String tenantUUID();

    String workplaceUUID();

    String groupUUID();
    /**
     * Raw role names as stored in the identity registry.
     * These are converted to Role enums inside SubjectFactory.
     */
    Set<String> roles();

    /**
     * License status for the identity.
     * Devices may have ACTIVE/EXPIRED/REVOKED.
     * All other identity types return NONE.
     */
    LicenceStatus licenceStatus();
}
