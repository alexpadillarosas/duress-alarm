package com.safecarealert.identity;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Production IdentityService implementation.
 * <p>
 * This service will eventually load IdentityRecord instances from a
 * persistent store (database, distributed cache, or identity microservice).
 * <p>
 * IMPORTANT:
 *  - Application code MUST NOT call this service directly.
 *  - Only CustomIdentityAugmentor should invoke lookup().
 *  - Application logic must use Subject (from SecurityIdentity) instead.
 * <p>
 * TODO:
 *  - Inject repository once persistence layer is implemented.
 *  - Map DB entities → IdentityRecord implementations.
 */
@ApplicationScoped
public class IdentityServiceImpl implements IdentityService {

    @Inject
    Logger log;

//    private final Map<String, IdentityRecord> registry = new ConcurrentHashMap<>();

    @PostConstruct
    void initTestData() {
        // Example test identities
//        register(new DeviceIdentity("DESKTOP-CLIENT-01",
//                                    "T1",
//                                    "W1",
//                                    "G1",
//                                    Set.of(Role.DEVICE.name()),
//                                    LicenceStatus.ACTIVE));
//
//        register(new MonitorIdentity("MONITOR-10", "T1",
//                Set.of(Role.MONITOR.name()), Set.of("W1", "W2")));
    }

    @Override
    public IdentityRecord lookup(String subjectUUID, String tenantHint) {


//        log.info("lookup subjectUUID: " + subjectUUID + ", tenantHint: " + tenantHint +  " called");
//        //for now mockup
//        return registry.get(subjectUUID);

        // TODO: Replace with DB lookup
        // Example (future):
        //
        // return repo.findBySubject(subjectUUID)
        //            .map(entity -> mapToIdentityRecord(entity))
        //            .orElse(null);
        //
        return null;
    }

    @Override
    public void register(IdentityRecord record) {
//        registry.put(record.subjectUUID(), record);
        // Production systems should not allow arbitrary identity registration.
//        throw new UnsupportedOperationException(
//                "Identity registration is not supported in production. Use provisioning workflows."
//        );
    }

    @Override
    public void clear() {
        // No-op in production.
//        registry.clear();
    }

    /**
     * Example mapping method (to be implemented when DB entities exist).
     *
     * private IdentityRecord mapToIdentityRecord(IdentityEntity entity) {
     *     return switch (entity.type()) {
     *         case DEVICE -> new DeviceIdentity(
     *                 entity.subjectUUID(),
     *                 entity.tenantUUID(),
     *                 entity.workplaceUUID(),
     *                 entity.groupUUID(),
     *                 entity.roles(),
     *                 entity.licenceStatus()
     *         );
     *
     *         case MONITOR -> new MonitorIdentity(
     *                 entity.subjectUUID(),
     *                 entity.tenantUUID(),
     *                 entity.roles(),
     *                 entity.allowedWorkplaces()
     *         );
     *
     *         case ADMIN -> new AdminIdentity(
     *                 entity.subjectUUID(),
     *                 entity.roles()
     *         );
     *
     *         case SUPPORT -> new SupportIdentity(
     *                 entity.subjectUUID(),
     *                 entity.tenantUUID(),
     *                 entity.roles()
     *         );
     *     };
     * }
     */
}
