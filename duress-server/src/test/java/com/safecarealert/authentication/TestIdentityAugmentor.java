package com.safecarealert.authentication;

import com.safecarealert.identity.*;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
@Alternative
@Priority(1)
public class TestIdentityAugmentor implements SecurityIdentityAugmentor {

    @Inject
    Logger log;

    @Inject
    IdentityService identityService;

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {

        String principalName = identity.getPrincipal() != null
                ? identity.getPrincipal().getName()
                : "UNKNOWN";

        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder(identity);
        builder.setPrincipal(() -> principalName);

        IdentityRecord record = identityService.lookup(principalName, null);

        if (record == null) {
            log.errorf("❌ No IdentityRecord found for %s", principalName);
            return Uni.createFrom().item(builder.build());
        }

        // =====================
        // ADMIN
        // =====================
        if (record instanceof AdminIdentity) {

            builder.addRole(Role.ADMIN.name());

            builder.addAttribute(Claim.SUBJECT.name(),
                    new Subject(principalName, null,
                            Set.of(Role.ADMIN), null, null, null)
            );
        }

        // =====================
        // MONITOR
        // =====================
        else if (record instanceof MonitorIdentity monitor) {

            builder.addRole(Role.MONITOR.name());

            Set<String> allowedWorkplaces = extractAllowedWorkplaces(identity);

            builder.addAttribute(Claim.SUBJECT.name(),
                    new Subject(
                            principalName,
                            monitor.tenantUUID(),
                            Set.of(Role.MONITOR),
                            null,
                            null,
                            allowedWorkplaces
                    )
            );

            log.infof("✅ Monitor Subject built: %s workplaces=%s",
                    principalName, allowedWorkplaces);
        }

        // =====================
        // DEVICE
        // =====================
        else if (record instanceof DeviceIdentity device) {

            builder.addRole(Role.DEVICE.name());

            builder.addAttribute(Claim.SUBJECT.name(),
                    new Subject(
                            principalName,
                            device.tenantUUID(),
                            Set.of(Role.DEVICE),
                            device.workplaceUUID(),
                            Set.of(device.groupUUID()),
                            null
                    )
            );

            builder.addAttribute("TENANT_ID", device.tenantUUID());
            builder.addAttribute("WORKPLACE_ID", device.workplaceUUID());
            builder.addAttribute("GROUP_ID", device.groupUUID());

            log.infof("✅ Device Subject built: %s tenant=%s workplace=%s group=%s",
                    principalName,
                    device.tenantUUID(),
                    device.workplaceUUID(),
                    device.groupUUID());
        }

        // =====================
        // SUPPORT (if used)
        // =====================
        else if (record instanceof SupportIdentity support) {

            builder.addRole(Role.SUPPORT.name());

            builder.addAttribute(Claim.SUBJECT.name(),
                    new Subject(
                            principalName,
                            support.tenantUUID(),
                            Set.of(Role.SUPPORT),
                            null,
                            null,
                            null
                    )
            );
        }

        builder.addAttribute(Claim.LICENCE_STATUS.name(), record.licenceStatus());

        return Uni.createFrom().item(builder.build());
    }

    // =====================
    // JWT CLAIM EXTRACTION
    // =====================
    private Set<String> extractAllowedWorkplaces(SecurityIdentity identity) {

        JsonWebToken jwt = (JsonWebToken) identity.getPrincipal();

        Object claim = jwt.getClaim(Claim.ALLOWED_WORKPLACES.name());

        Set<String> allowedWorkplaces;

        if (claim instanceof Set<?> s) {
            allowedWorkplaces = s.stream()
                    .map(this::normalize)
                    .collect(Collectors.toSet());

        } else if (claim instanceof java.util.Collection<?> c) {
            allowedWorkplaces = c.stream()
                    .map(this::normalize)
                    .collect(Collectors.toSet());

        } else if (claim instanceof String str) {
            allowedWorkplaces = Set.of(normalize(str));

        } else {
            allowedWorkplaces = Set.of();
        }

        log.infof("✅ Extracted allowedWorkplaces from JWT: %s (raw=%s)",
                allowedWorkplaces, claim);

        return allowedWorkplaces;
    }

    private String normalize(Object o) {
        String value = String.valueOf(o).trim();

        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }

        return value;
    }
}
