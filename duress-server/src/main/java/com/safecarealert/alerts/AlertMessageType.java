package com.safecarealert.alerts;

/**
 * AlertMessageType
 * ----------------
 * Feature-scoped subset of MessageType.
 *
 * NOTE:
 *   This does NOT replace MessageType.
 *   It simply provides a clean boundary for alert-related logic.
 */
public enum AlertMessageType {
    ALERT_START,
    ALERT_STOP
}
