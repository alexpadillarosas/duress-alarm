package com.safecarealert.system;

import com.safecarealert.alerts.registry.GroupRegistry;
import com.safecarealert.websocket.AlertSocket;
import io.quarkus.runtime.ShutdownEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * ShutdownManager
 * ---------------
 * Centralized shutdown coordinator.
 *
 * This is the ONLY class that observes ShutdownEvent.
 * All subsystems delegate shutdown behavior to this class.
 */
@ApplicationScoped
public class ShutdownManager {

    @Inject
    Logger log;

    @Inject
    AlertSocket alertSocket;

    @Inject
    GroupRegistry groupRegistry;

    public void onShutdown(@Observes ShutdownEvent ev) {
        log.info("📢 [SYSTEM] Shutdown detected. Coordinating shutdown...");

        // 1️⃣ Stop accepting new messages
        alertSocket.prepareForShutdown();

        // 2️⃣ Notify all connected devices
        groupRegistry.prepareForShutdown();

        log.info("✅ ShutdownManager: All subsystems notified.");
    }
}
