package com.safecarealert.authentication;

import com.safecarealert.identity.Claim;
import com.safecarealert.identity.Role;
import com.safecarealert.identity.TokenGeneratorResource;
import com.safecarealert.utils.StringUtils;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.smallrye.jwt.auth.principal.JWTParser;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(IdentityTestProfile.class)
class TokenGeneratorResourceTest {

    @Inject
    @ConfigProperty(name = "jwt.issuer")
    String jwtIssuer;

    @Inject
    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String verifyIssuer;

    @Inject
    TokenGeneratorResource tokenGeneratorResource;

    @Inject
    JWTParser jwtParser;

    @Inject
    IdentityTestHelper id;

    @BeforeEach
    void setup() {
        id.clear();
    }

    @Test
    void issuerValuesMustMatch() {
        assertNotNull(jwtIssuer);
        assertNotNull(verifyIssuer);
        assertEquals(jwtIssuer, verifyIssuer);
    }

    // ------------------------------------------------------------
    // DEVICE TEST
    // ------------------------------------------------------------
    @Test
    void deviceTokenHasExpectedClaims() throws Exception {
        id.identity("DEV-123")
                .tenant("T-UUID-1")
                .workplace("W-UUID-1")
                .group("G-UUID-1")
                .roles(Role.DEVICE.name())
                .register();

        Response response = tokenGeneratorResource.generate("T-UUID-1", "DEV-123");
        String token = response.readEntity(TokenGeneratorResource.TokenResponse.class).token();

        JsonWebToken jwt = jwtParser.parse(token);

        assertEquals("DEV-123", jwt.getSubject());
        assertTrue(jwt.getGroups().contains(Role.DEVICE.name()));

        // ✅ Present (generator includes them)
        assertEquals("W-UUID-1", jwt.getClaim(Claim.WORKPLACE_UUID.name()));

        Object groupClaim = jwt.getClaim(Claim.GROUP_UUID.name());

        Set<String> groups = ((java.util.Collection<?>) groupClaim)
                .stream()
                .map(StringUtils::normalize)
                .collect(Collectors.toSet());

        assertTrue(groups.contains("G-UUID-1"));

        // ✅ Not used anymore
        assertNull(jwt.getClaim(Claim.SUBJECT_UUID.name()));
        assertNull(jwt.getClaim(Claim.ALLOWED_WORKPLACES.name()));
    }

    // ------------------------------------------------------------
    // SYSTEM_ADMIN TEST
    // ------------------------------------------------------------
    @Test
    void systemAdminOnlyRequiresSubjectId() throws Exception {
        id.identity("SYS-999")
                .roles(Role.ADMIN.name())
                .register();

        Response response = tokenGeneratorResource.generate(null, "SYS-999");
        String token = response.readEntity(TokenGeneratorResource.TokenResponse.class).token();

        JsonWebToken jwt = jwtParser.parse(token);

        assertEquals("SYS-999", jwt.getSubject());
        assertTrue(jwt.getGroups().contains(Role.ADMIN.name()));

        // ✅ Admin has no tenant/workplace/group
        assertNull(jwt.getClaim(Claim.TENANT_UUID.name()));
        assertNull(jwt.getClaim(Claim.WORKPLACE_UUID.name()));
        assertNull(jwt.getClaim(Claim.GROUP_UUID.name()));
        assertNull(jwt.getClaim(Claim.SUBJECT_UUID.name()));
    }

    // ------------------------------------------------------------
    // MONITOR TEST
    // ------------------------------------------------------------
    @Test
    void monitorRequiresTenantAndWorkplaces() throws Exception {
        id.identity("MON-777")
                .tenant("T-UUID-5")
                .roles(Role.MONITOR.name())
                .monitorWorkspaces("W1", "W2")
                .register();

        Response response = tokenGeneratorResource.generate("T-UUID-5", "MON-777");
        String token = response.readEntity(TokenGeneratorResource.TokenResponse.class).token();

        JsonWebToken jwt = jwtParser.parse(token);

        assertEquals("MON-777", jwt.getSubject());
        assertTrue(jwt.getGroups().contains(Role.MONITOR.name()));

        // ✅ Generator includes tenant + allowed workplaces
        assertEquals("T-UUID-5", jwt.getClaim(Claim.TENANT_UUID.name()));

        Object allowed = jwt.getClaim(Claim.ALLOWED_WORKPLACES.name());
        Set<String> workplaces = ((java.util.Collection<?>) allowed)
                .stream()
                .map(StringUtils::normalize)
                .collect(Collectors.toSet());

        assertTrue(workplaces.contains("W1"));
        assertTrue(workplaces.contains("W2"));

        // ✅ Not applicable to monitors
        assertNull(jwt.getClaim(Claim.WORKPLACE_UUID.name()));
        assertNull(jwt.getClaim(Claim.GROUP_UUID.name()));
        assertNull(jwt.getClaim(Claim.SUBJECT_UUID.name()));
    }
}
