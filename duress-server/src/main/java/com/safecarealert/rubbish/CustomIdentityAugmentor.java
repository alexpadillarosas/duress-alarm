package com.safecarealert.rubbish;

import com.safecarealert.identity.*;
import com.safecarealert.identity.authentication.JWTAugmentor;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

/**
 *[ Client Request ]
 *        │
 *        ▼
 *  1. Quarkus Crypto Layer ──► Verifies signature against publicKey.pem and checks expiration.
 *        │
 *        ▼ (If Crypto Passes)
 *  2. Authentication Layer ──► Parses raw JWT claims (e.g., subject, groups/roles).
 *        │
 *        ▼
 *  3. CustomIdentityAugmentor ◄── CLIMAX: Quarkus invokes your augmentor HERE.
 *        │
 *        ▼
 *  4. Business Logic Layer ──► Reaches your Filters, Annotations (@RolesAllowed), and Sockets.
 * <p>
 *     IMPORTANT, TODO for licenses
 * Every time a client submits a valid token, Quarkus handles the signature verification first.
 * Once it establishes that the token is valid, it wraps the base claims into an initial SecurityIdentity object.
 * Before passing control to your application endpoints, Quarkus sweeps the CDI container for any beans implementing
 * SecurityIdentityAugmentor.
 * It hands them this temporary identity so you can run database lookups to add extra contextual properties
 * (like pulling a user's workplace, licensing rules, or granular feature flags) before final authorization happens.
 *
 * These are exactly what it’s meant for:
 *
 * ✅ Load extra identity data from DB
 * ✅ Enrich SecurityIdentity → Subject
 * ✅ Validate identity constraints (e.g., tenant exists, license valid)
 * ✅ Add attributes (tenant, workplace, permissions, etc.)
 *
 * Avoid:
 *
 * ❌ heavy blocking DB queries on hot path without care
 * ❌ business workflows
 * ❌ long-running logic
 * ❌ side effects (writes)
 */

@ApplicationScoped
public class CustomIdentityAugmentor implements SecurityIdentityAugmentor {

    @Inject
    Logger log;

    @Inject
    JWTValidator jwtValidator;

    @Inject
    JWTAugmentor jwtAugmentor;

    @Inject
    IdentityParser identityParser;

    @Inject
    IdentityService identityService;

    @Inject
    SubjectFactory subjectFactory;

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {

        if (identity.isAnonymous() || !(identity.getPrincipal() instanceof JsonWebToken jwt)) {
            return Uni.createFrom().item(identity);
        }

        if (!jwtValidator.validate(identity)) {
            log.warn("❌ JWTValidator rejected token — identity not augmented");
            return Uni.createFrom().item(identity);
        }

        jwtAugmentor.augment(jwt);

        String subjectUUID = identityParser.parseSubject(jwt.getSubject());
        String tenantUUID  = identityParser.parseTenant(jwt.getClaim(Claim.TENANT_UUID.name()));

        IdentityRecord record = identityService.lookup(subjectUUID, tenantUUID);
        if (record == null) {
            log.warnf("❌ No IdentityRecord found for subject=%s tenant=%s", subjectUUID, tenantUUID);
            return Uni.createFrom().item(identity);
        }

        Subject subject = subjectFactory.fromIdentityRecord(record);

        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder(identity);

        builder.addAttribute(Claim.SUBJECT_UUID.name(), subjectUUID);
        builder.addAttribute(Claim.SUBJECT.name(), subject);
        builder.addAttribute(Claim.LICENCE_STATUS.name(), record.licenceStatus());

        switch (record) {
            case DeviceIdentity d -> {
                builder.addAttribute(Claim.TENANT_UUID.name(), d.tenantUUID());
                builder.addAttribute(Claim.WORKPLACE_UUID.name(), d.workplaceUUID());
                builder.addAttribute(Claim.GROUP_UUID.name(), d.groupUUID());
            }
            case MonitorIdentity m -> {
                builder.addAttribute(Claim.TENANT_UUID.name(), m.tenantUUID());
                builder.addAttribute(Claim.ALLOWED_WORKPLACES.name(), m.allowedWorkplaces());
            }
            case AdminIdentity a -> {}
            case SupportIdentity s -> {
                builder.addAttribute(Claim.TENANT_UUID.name(), s.tenantUUID());
            }
        }

        log.infov("🆔 Identity augmented: subject={0}, roles={1}", subjectUUID, identity.getRoles());

        return Uni.createFrom().item(builder.build());
    }
}
