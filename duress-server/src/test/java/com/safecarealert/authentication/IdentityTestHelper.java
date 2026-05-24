package com.safecarealert.authentication;


import com.safecarealert.identity.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Set;

/**
 * Helper class using fluent builder for registering Identities into an In memory map
 */
@ApplicationScoped
public class IdentityTestHelper {

    @Inject
    Logger log;

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    @Inject
    IdentityService identityService;

    public IdentityRecordBuilder identity(String subjectUUID) {
        return new IdentityRecordBuilder(identityService, subjectUUID);
    }

    public void clear() {
        identityService.clear();
    }

    public static class IdentityRecordBuilder {
        private final IdentityService service;
        private final String subjectUUID;

        private String tenantUUID;
        private String workplaceUUID;
        private String groupUUID;

        private Set<String> roles = Set.of();
        private Set<String> monitorWorkspaces = Set.of();

        IdentityRecordBuilder(IdentityService service, String subjectUUID) {
            this.service = service;
            this.subjectUUID = subjectUUID;
        }

        public IdentityRecordBuilder tenant(String tenantUUID) {
            this.tenantUUID = tenantUUID;
            return this;
        }

        public IdentityRecordBuilder workplace(String workplaceUUID) {
            this.workplaceUUID = workplaceUUID;
            return this;
        }

        public IdentityRecordBuilder group(String groupUUID) {
            this.groupUUID = groupUUID;
            return this;
        }

        public IdentityRecordBuilder roles(String... roles) {
            this.roles = Set.of(roles);
            return this;
        }

        public IdentityRecordBuilder monitorWorkspaces(String... workspaces) {
            this.monitorWorkspaces = Set.of(workspaces);
            return this;
        }


        public void register() {

            // Identity invariants (BONUS VALIDATIONS)
            //  1. Devices must NOT have monitorWorkspaces
            if (roles.contains(Role.DEVICE.name()) && !monitorWorkspaces.isEmpty()) {
                throw new IllegalStateException( "Devices cannot have monitorWorkspaces. " + "Use workplace() + group() instead." );
            }
            // 2. Monitors must NOT have workplaceUUID or groupUUID
            if (roles.contains(Role.MONITOR.name()) && (workplaceUUID != null || groupUUID != null)) {
                throw new IllegalStateException( "Monitors cannot have workplaceUUID or groupUUID. " + "Use monitorWorkspaces() instead." );
            }

            IdentityRecord record;

            if (roles.contains(Role.DEVICE.name())) {
                record = new DeviceIdentity(
                        subjectUUID,
                        tenantUUID,
                        workplaceUUID,
                        groupUUID,
                        roles,
                        LicenceStatus.ACTIVE
                );

            } else if (roles.contains(Role.MONITOR.name())) {
                record = new MonitorIdentity(
                        subjectUUID,
                        tenantUUID,
                        roles,
                        monitorWorkspaces
                );

            } else if (roles.contains(Role.ADMIN.name())) {
                record = new AdminIdentity(
                        subjectUUID,
                        roles
                );

            } else if (roles.contains(Role.SUPPORT.name())) {
                record = new SupportIdentity(
                        subjectUUID,
                        tenantUUID,
                        roles
                );

            } else {
                throw new IllegalStateException("Unknown identity type for roles: " + roles);
            }

            service.register(record);
        }

    }

//    /**
//     * Builds a JWT for the given identity.
//     */
//    public String tokenFor(String tenantUUID, String subjectUUID) {
//
//        IdentityRecord rec = identityService.lookup(subjectUUID, tenantUUID);
//        if (rec == null) {
//            log.errorf("Token generation failed: Identity not registered for Subject UUID %s in Tenant %s", subjectUUID, tenantUUID);
//            throw new IllegalStateException("Identity not registered: " + subjectUUID);
//        }
//        log.infov("Generating test JWT for user: {0} with roles/groups: {1}", rec.subjectUUID(), rec.roles());
//        var jwt = Jwt.issuer(issuer)
//                .subject(rec.subjectUUID())
//                .upn(rec.subjectUUID())
//                .groups(rec.roles())
//                .issuedAt(Instant.now())
//                .expiresIn(Duration.ofHours(24));
//
//        if (rec.tenantUUID() != null) {
//            jwt = jwt.claim(Claim.TENANT_UUID.name(), rec.tenantUUID());
//        }
//
//        if (rec instanceof DeviceIdentity device) {
//            log.debugf("Mapping DeviceIdentity claims for Device: %s", rec.subjectUUID());
//            jwt = jwt.claim(Claim.WORKPLACE_UUID.name(), device.workplaceUUID());
//            jwt = jwt.claim(Claim.GROUP_UUID.name(), device.groupUUID());
//            jwt = jwt.claim(Claim.LICENCE_STATUS.name(), device.licenceStatus().name());
//        }
//
//        if (rec instanceof MonitorIdentity monitor) {
//            log.debugf("Mapping MonitorIdentity claims for Monitor: %s", rec.subjectUUID());
//            jwt = jwt.claim(Claim.ALLOWED_WORKPLACES.name(), monitor.allowedWorkplaces());
//        }
//
//        if (rec instanceof AdminIdentity admin) {
//            log.infof("Mapping AdminIdentity claims for Administrator: %s", rec.subjectUUID());
////            jwt = jwt.claim(Claim.)
//
//        }
//        log.debug("JWT successfully constructed and signed.");
//        return jwt.sign();
//
//    }
}
