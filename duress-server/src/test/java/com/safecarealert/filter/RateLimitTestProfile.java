package com.safecarealert.filter;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;


public class RateLimitTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides(){
        return Map.of(
                "app.security.ratelimit.limit", "1",
                "app.security.ratelimit.window-seconds", "60");
    }
}
