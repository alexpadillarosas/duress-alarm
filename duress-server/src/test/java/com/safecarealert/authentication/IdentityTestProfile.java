package com.safecarealert.authentication;



import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

/**
 *  IdentityTestProfile :
 *  We have override beansthis config override
 * Production:
 *     Jwt → SecurityIdentity → ProductionAugmentor → Subject
 * Test:
 *     TestIdentityService → TestTokenGenerator → TestIdentityAugmentor → Subject
 *
 */
public class IdentityTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "jwt.issuer", "https://safetycarealert.com/issuer",
                "quarkus.arc.selected-alternatives", "com.duressalert.authentication.TestIdentityService, com.duressalert.authentication.TestIdentityAugmentor"

                // Prevent HTTP auth from trying to authenticate
//                "quarkus.http.auth.proactive", "false",

                // Disable SmallRye JWT completely
//                "mp.jwt.verify.publickey.location", "none",
//                "mp.jwt.verify.issuer", "none"

                );
    }
}
