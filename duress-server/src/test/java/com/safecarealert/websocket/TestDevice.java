package com.safecarealert.websocket;


import com.safecarealert.messages.MessageType;
import com.safecarealert.utils.JsonService;
import io.quarkus.logging.Log;
import io.quarkus.websockets.next.CloseReason;
import io.quarkus.websockets.next.WebSocketClientConnection;

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * TestDevice
 * ----------
 * A test helper representing a connected WebSocket client (DEVICE or MONITOR).
 *
 * Responsibilities:
 *   - Hold the WebSocket connection
 *   - Provide message polling utilities
 *   - Provide ACK waiting utilities
 *   - Provide close-reason inspection
 *   - Provide helper methods for sending alert messages
 *
 * This class is used by all WebSocket integration tests.
 */
public record TestDevice(
        String clientId,                                // DEVICE ID or MONITOR ID
        WebSocketClientConnection connection,           // Active WebSocket connection
        LinkedBlockingQueue<String> messages,           // Incoming message queue
        JsonService jsonService,                        // JSON helper
        AtomicReference<CloseReason> closeReason        // Close reason holder
) {

    private ClientMessageParser parser() {
        return new ClientMessageParser(jsonService);
    }
    // -------------------------------------------------------------------------
    // MESSAGE POLLING
    // -------------------------------------------------------------------------

    /**
     * Polls the next DeviceMessage from the queue.
     *
     * @param timeout how long to wait
     * @param unit    time unit
     * @return parsed DeviceMessage or null if timeout
     */
    public ClientDeviceMessage pollMessage(long timeout, TimeUnit unit) throws InterruptedException {
        String raw = messages.poll(timeout, unit);
        if (raw == null) return null;

        Log.tracev("Client[{0}] Received Raw JSON: {1}", clientId, raw);

        try {
            return parser().parse(raw);
        } catch (Exception e) {
            Log.errorv("JSON ERROR for client {0}: {1}", clientId, e.getMessage());
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // WAIT FOR SPECIFIC MESSAGE TYPE
    // -------------------------------------------------------------------------

    /**
     * Waits until a message of the given type arrives.
     *
     * @param type the expected MessageType
     * @return the matching DeviceMessage
     */
    public ClientDeviceMessage waitForMessage(MessageType type) {
        try {
            return org.awaitility.Awaitility.await()
                    .atMost(5, TimeUnit.SECONDS)
                    .until(() -> this.pollMessage(100, TimeUnit.MILLISECONDS),
                            msg -> msg != null && msg.messageType() == type);
        } catch (org.awaitility.core.ConditionTimeoutException e) {
            throw new AssertionError(
                    "Timed out waiting for message type " + type + ". Check server logs for details.",
                    e
            );
        }
    }

    // -------------------------------------------------------------------------
    // CLOSE HANDLING
    // -------------------------------------------------------------------------

    /**
     * Closes the WebSocket connection.
     */
    public void close() {
        connection.closeAndAwait();
    }

    /**
     * Returns the close code if available.
     */
    public Integer getCloseCode() {
        return closeReason.get() != null ? (int) closeReason.get().getCode() : null;
    }

    /**
     * Returns the close reason message if available.
     */
    public String getCloseReasonMessage() {
        return closeReason.get() != null ? closeReason.get().getMessage() : null;
    }

    /**
     * Waits for the connection to close and returns the CloseReason.
     */
    public CloseReason waitForCloseReason() {
        return org.awaitility.Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .until(closeReason::get, java.util.Objects::nonNull);
    }

    // -------------------------------------------------------------------------
    // MESSAGE SENDING HELPERS
    // -------------------------------------------------------------------------

    /**
     * Sends an ALERT_START message.
     */
    public void sendStartAlertToGroup(String location, String person, String message) {

        Map<String, Object> alert = DeviceMessageBuilder
                .create(MessageType.ALERT_START)
                .put("location", location)
                .put("person", person)
                .put("alertOwner", clientId)
                .put("message", message)
                .withAutoCorrelationId()
                .build();

//        Map<String, String> data = Map.of(
//                "location", location,
//                "person", person,
//                "alertOwner", clientId,
//                "message", message
//        );
//
//        DeviceMessage alert = new DeviceMessage(
//                MessageType.ALERT_START,
//                data
//        );

        connection.sendTextAndAwait(jsonService.toJson(alert));
    }

    /**
     * Sends an ALERT_STOP message.
     */
    public void sendStopAlertToGroup(String alertOwner, String correlationId) {

//        DeviceMessage stop = new DeviceMessage(
//                MessageType.ALERT_STOP,
//                Map.of(
//                        "alertOwner", alertOwner,
//                        "deviceId", clientId
//                ),
//                correlationId
//        );
        Map<String, Object> stop = DeviceMessageBuilder
                .create(MessageType.ALERT_STOP)
                .put("alertOwner", alertOwner)
                .withCorrelationId(correlationId)
                .build();

        connection.sendTextAndAwait(jsonService.toJson(stop));
    }

    // -------------------------------------------------------------------------
    // UTILITY
    // -------------------------------------------------------------------------

    /**
     * Clears all pending messages from the queue.
     */
    public void clearMessages() {
        messages.clear();
    }
}
