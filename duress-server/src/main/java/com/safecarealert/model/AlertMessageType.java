package com.safecarealert.model;

public enum AlertMessageType {
    DURESS_TRIGGER,     // Initial alarm trigger
    ACKNOWLEDGMENT,     // Someone acknowledged the alarm
    UPDATE,             // Status update / additional info
    ESCALATION,         // Escalated to higher authority
    RESOLUTION,         // Final resolution
    FALSE_ALARM,        // Marked as false alarm
    CANCEL,             // Cancelled by user
    SYSTEM_NOTE,        // System generated message
    LOCATION_UPDATE
}