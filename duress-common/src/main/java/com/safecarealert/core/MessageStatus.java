package com.safecarealert.core;

/**
 * MessageStatus
 * -------------
 * Represents the outcome of alert lifecycle commands.
 *
 * These statuses are used inside MESSAGE_ACK payloads to indicate
 * whether an alert was started, stopped, or ignored due to idempotency.
 *
 * This enum intentionally contains ONLY alert lifecycle outcomes.
 * Connection/session results and internal log markers have been removed.
 */
public enum MessageStatus {

        // --- Alert Lifecycle Results ---

        /** Alert successfully started and broadcast to peers */
        BROADCAST_COMPLETE,

        /** Alert successfully stopped and broadcast to peers */
        STOP_COMPLETE,

        /** Attempted to start an alert that is already active */
        ALERT_ALREADY_STARTED,

        /** Attempted to stop an alert that is not active */
        ALERT_ALREADY_STOPPED
}
