package com.safecarealert.identity.authentication;

import com.safecarealert.identity.Claim;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

@ApplicationScoped
public class JWTAugmentor {

    @Inject
    Logger log;

    public void augment(JsonWebToken jwt) {

        Object tenant = jwt.getClaim(Claim.TENANT_UUID.name());
        if (tenant != null) {
            String normalized = tenant.toString().trim().toUpperCase();
            log.debugf("🔧 Normalized tenantUUID: %s → %s", tenant, normalized);
        }

        if (jwt.getGroups().contains("MONITOR")) {
            log.debug("🔧 Derived claim: isMonitor=true");
        }
    }
}
