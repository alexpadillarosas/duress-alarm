package com.safecarealert;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ApplicationLifecycleBean {
    private static final Logger LOGGER = Logger.getLogger("AppLifecycle");

    @ConfigProperty(name = "app.version")
    String version;

    void onStart(@Observes StartupEvent ev) {
        LOGGER.info("--------------------------------------------------");
        LOGGER.info("  APPLICATION IS STARTING");
        LOGGER.info("  Version: " + version);
        LOGGER.info("--------------------------------------------------");
    }
}
