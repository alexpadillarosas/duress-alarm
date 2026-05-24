package com.safecarealert;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

@Liveness
@ApplicationScoped
public class VersionHealthCheck implements HealthCheck {

//    @ConfigProperty(name = "quarkus.application.version", defaultValue = "1.0.0")
//    String version;

    @ConfigProperty(name = "app.version")
    String version;

//    @PostConstruct
//    void init(){



    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("Version Check")
                .up()
                .withData("app_version", version)
                .build();
    }
}
