package com.safecarealert.identity;

/**
 * IdentityService is the internal identity lookup mechanism.
 * <p>
 * It is responsible for retrieving the server‑truth IdentityRecord
 * for a given subject UUID. This may be backed by:
 *  - an in‑memory registry (tests)
 *  - a database (production)
 *  - a cache layer (future)
 * <p>
 * IMPORTANT:
 *  - Application code MUST NOT call this service directly.
 *  - Application code MUST use Subject (from SecurityIdentity) instead.
 *  - Only CustomIdentityAugmentor and SubjectFactory should depend on this.
 */
public interface IdentityService {

    /**
     * Lookup the identity record for the given subject UUID.
     *
     * @param subjectUUID the unique identifier of the identity
     * @param tenantHint  optional tenant hint extracted from JWT
     *                    (can be null for system admin or monitor identities)
     *
     * @return the IdentityRecord, or null if not found
     */
    IdentityRecord lookup(String subjectUUID, String tenantHint);

    /**
     * Register an identity record.
     * Used only in:
     *  - TestIdentityService
     *  - Admin UI (if applicable)
     */
    void register(IdentityRecord record);

    /**
     * Clear the identity registry.
     * Used only in tests.
     */
    void clear();
}
