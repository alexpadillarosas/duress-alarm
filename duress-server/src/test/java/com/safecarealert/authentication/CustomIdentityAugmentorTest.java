package com.safecarealert.authentication;

import com.safecarealert.identity.*;

import com.safecarealert.rubbish.CustomIdentityAugmentor;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that the CustomIdentityAugmentor correctly maps JWT claims
 * into SecurityIdentity attributes.
 */
//@QuarkusTest
//@TestProfile(IdentityTestProfile.class)
class CustomIdentityAugmentorTest {

    @Inject
    TokenGeneratorResource tokenGeneratorResource;

    @Inject
    CustomIdentityAugmentor augmentor;

    @Inject
    JWTParser jwtParser;

    @Inject
    IdentityService identityService;

    @BeforeEach
    void setup() {
        identityService.clear();
    }

    /**
     * Converts a raw JWT string into a Quarkus SecurityIdentity.
     */
    private SecurityIdentity identityFromToken(String token) throws ParseException {
        JsonWebToken jwt = jwtParser.parse(token);
        return QuarkusSecurityIdentity.builder()
                .setPrincipal(jwt)
                .addRoles(jwt.getGroups())
                .build();
    }

//    @Test
    void augmentorPopulatesDeviceAttributes() throws ParseException {
        // Arrange: register identity in the in-memory registry
        identityService.register(
                new DeviceIdentity(
                        "DEV-123",
                        "TENANT-UUID-1",
                        "W-UUID-1",
                        "G-UUID-1",
                        Set.of(Role.DEVICE.name()),
                        LicenceStatus.ACTIVE
                )
                /*
                new IdentityRecord(
                        "TENANT-UUID-1",
                        "W-UUID-1",
                        "G-UUID-1",
                        "DEV-123",
                        Set.of(Roles.DEVICE.name()),
                        Set.of("W-UUID-1", "W-UUID-2"),
                        LicenceStatus.ACTIVE
                )

                 */
        );

        // Act: generate token
        Response response = tokenGeneratorResource.generate("TENANT-UUID-1", "DEV-123");
        String token = response.readEntity(TokenGeneratorResource.TokenResponse.class).token();

        SecurityIdentity base = identityFromToken(token);
        SecurityIdentity augmented = augmentor.augment(base, null).await().indefinitely();

        // Assert: attributes correctly mapped
        assertEquals("DEV-123", augmented.getAttribute(Claim.SUBJECT_UUID.name()));
        assertEquals("TENANT-UUID-1", augmented.getAttribute(Claim.TENANT_UUID.name()));
        assertEquals("W-UUID-1", augmented.getAttribute(Claim.WORKPLACE_UUID.name()));
        assertEquals("G-UUID-1", augmented.getAttribute(Claim.GROUP_UUID.name()));
        assertTrue(augmented.getRoles().contains(Role.DEVICE.name()));
    }

//    @Test
    void augmentorPopulatesSystemAdminAttributes() throws ParseException {
        // Arrange: register SYSTEM_ADMIN identity
        identityService.register(
                new AdminIdentity(
                        "SYS-999",
                        Set.of(Role.ADMIN.name())
                )
                /*
                new IdentityRecord(
                        null,
                        null,
                        null,
                        "SYS-999",
                        Set.of(Roles.SYSTEM_ADMIN.name()),
                        null,
                        LicenceStatus.NONE
                )

                 */
        );

        // Act: generate token (tenantUUID is null for system admin)
        Response response = tokenGeneratorResource.generate(null, "SYS-999");
        String token = response.readEntity(TokenGeneratorResource.TokenResponse.class).token();

        SecurityIdentity base = identityFromToken(token);
        SecurityIdentity augmented = augmentor.augment(base, null).await().indefinitely();

        // Assert: only subjectUUID is present
        assertEquals("SYS-999", augmented.getAttribute(Claim.SUBJECT_UUID.name()));
        assertNull(augmented.getAttribute(Claim.TENANT_UUID.name()));
        assertNull(augmented.getAttribute(Claim.WORKPLACE_UUID.name()));
        assertNull(augmented.getAttribute(Claim.GROUP_UUID.name()));
        assertNull(augmented.getAttribute(Claim.ALLOWED_WORKPLACES.name()));
        assertTrue(augmented.getRoles().contains(Role.ADMIN.name()));
    }
}
