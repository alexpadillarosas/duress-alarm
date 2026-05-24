package com.safecarealert.websocket;

import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * ConnectionEvents
 * ----------------
 * Centralised event hooks for WebSocket lifecycle events.
 *
 * This class does NOT replace @OnOpen/@OnClose handlers.
 * Instead, it provides a clean feature-based place to:
 *   - log connection events
 *   - trigger analytics
 *   - trigger audit trails
 *   - notify other subsystems
 *
 * Currently used as a placeholder for future expansion.
 */
@ApplicationScoped
public class ConnectionEvents {

    @Inject
    Logger log;

    public void onDeviceConnected(WebSocketConnection connection, String deviceId) {
        log.infov("📡 [EVENT] Device connected: id={0}, device={1}", connection.id(), deviceId);
    }

    public void onDeviceDisconnected(WebSocketConnection connection, String deviceId) {
        log.infov("📡 [EVENT] Device disconnected: id={0}, device={1}", connection.id(), deviceId);
    }
}
