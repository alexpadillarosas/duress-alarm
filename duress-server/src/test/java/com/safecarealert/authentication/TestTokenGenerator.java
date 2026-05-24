package com.safecarealert.authentication;

import com.safecarealert.identity.*;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;

/**
 * Given a subjectID and optionally a tenantID, it will build a JWT token using the data registered
 * in the map by TestIdentityService (only when testing, this class is called at injection time instead of
 * IdentityService (check IdentityTestProfile)
 */
@ApplicationScoped
public class TestTokenGenerator {
    @Inject
    Logger log;

    @Inject
    IdentityService identityService;

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    /**
     * Looks up the registered identity in the test environment (in memory map)
     * and compiles a signed JWT with matching typed claims.
     */
    public String tokenFor(String tenantUUID, String subjectUUID) {
        log.debugf("Extracting token for Tenant: %s, Subject: %s", tenantUUID, subjectUUID);

        IdentityRecord rec = identityService.lookup(subjectUUID, tenantUUID);
        if (rec == null) {
            log.errorf("Token generation failed: Identity not registered for Subject UUID %s in Tenant %s", subjectUUID, tenantUUID);
            throw new IllegalStateException("Identity not registered: " + subjectUUID);
        }

        log.infov("Generating test JWT for user: {0} with roles/groups: {1}", rec.subjectUUID(), rec.roles());

        var jwt = Jwt.issuer(issuer)
                .subject(rec.subjectUUID())
                .upn(rec.subjectUUID())
                .groups(rec.roles())
                .issuedAt(Instant.now())
                .expiresIn(Duration.ofHours(24));

        if (rec.tenantUUID() != null) {
            jwt = jwt.claim(Claim.TENANT_UUID.name(), rec.tenantUUID());
        }

        if (rec instanceof DeviceIdentity device) {
            log.debugf("Mapping DeviceIdentity claims for Device: %s", rec.subjectUUID());
            jwt = jwt.claim(Claim.WORKPLACE_UUID.name(), device.workplaceUUID());
            jwt = jwt.claim(Claim.GROUP_UUID.name(), device.groupUUID());
            jwt = jwt.claim(Claim.LICENCE_STATUS.name(), device.licenceStatus().name());
        }

        if (rec instanceof MonitorIdentity monitor) {
            log.debugf("Mapping MonitorIdentity claims for Monitor: %s", rec.subjectUUID());
            jwt = jwt.claim(Claim.ALLOWED_WORKPLACES.name(), monitor.allowedWorkplaces());
        }

        if (rec instanceof AdminIdentity admin) {
            log.infof("Mapping AdminIdentity claims for Administrator: %s", rec.subjectUUID());
//            jwt = jwt.claim("is_system_admin", true);
        }

        String signedToken = jwt.sign();
        log.debug("JWT successfully constructed and signed.");
        return signedToken;
    }
}

