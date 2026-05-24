package com.safecarealert.rubbish;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

@ApplicationScoped
public class JWTValidator {

    @Inject
    Logger log;

    public boolean validate(SecurityIdentity identity) {

        //Validate the JWT sent is a JWT
        if (!(identity.getPrincipal() instanceof JsonWebToken jwt)) {
            log.error("❌ JWTValidator: Principal is not a JsonWebToken");
            return false;
        }
        //Validate it has a subject
        if (jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            log.error("❌ JWT missing subject");
            return false;
        }
        //Validate it has roles
        if (identity.getRoles().isEmpty()) {
            log.error("❌ JWT contains no roles");
            return false;
        }

//        if (!identity.getRoles().contains("ADMIN")) {
//            if (!jwt.containsClaim(Claim.TENANT_UUID.name())) {
//                log.error("❌ JWT missing tenantUUID for non-admin identity");
//                return false;
//            }
//        }

        log.debug("🔐 JWTValidator: token is semantically valid");
        return true;
    }
}
