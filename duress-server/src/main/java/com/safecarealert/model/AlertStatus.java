package com.safecarealert.model;

public enum AlertStatus {
    ACTIVE,
    IN_PROGRESS,
    ACKNOWLEDGED,
    CANCELLED,
    RESOLVED,
    FALSE_ALARM,
    EXPIRED
}
/*
 * Status,          Meaning,                                    Should be used for
 * ACTIVE,          Alarm just triggered,                       New duress alarm
 * ACKNOWLEDGED,    Someone saw it and took ownership,          Very important
 * IN_PROGRESS,     Response is underway,                       Good to have
 * RESOLVED,        Alarm handled and closed,                   Final state
 * CANCELLED,       User cancelled it,                          Self-cancel
 * FALSE_ALARM,     Confirmed false alarm,                      Important for stats
 * EXPIRED,         Auto-closed after timeout,                  System cleanup
 */