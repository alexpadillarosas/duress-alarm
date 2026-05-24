package com.safecarealert.websocket;

import com.safecarealert.core.ClientType;

/**
 * Sealed interface for clean separation between Device and Monitor connections.
 */
public sealed interface ConnectionContext permits DeviceContext, MonitorContext {
    String tenantId();
    String clientId();           // deviceId or monitorId
    ClientType clientType();

    default boolean isDevice() {
        return clientType() == ClientType.DEVICE;
    }

    default boolean isMonitor() {
        return clientType() == ClientType.MONITOR;
    }
}

