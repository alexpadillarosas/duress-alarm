package com.safecarealert.system;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Config
 * ------
 * Centralised typed configuration access.
 *
 * This class wraps application.properties values in a clean, testable way.
 */
@ApplicationScoped
public class Config {

    @ConfigProperty(name = "app.version")
    public String appVersion;

    @ConfigProperty(name = "jwt.issuer")
    public String jwtIssuer;

    // Add more config values as needed
}
