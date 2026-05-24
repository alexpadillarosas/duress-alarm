package com.safecarealert.websocket;

import com.safecarealert.core.ClientType;
import com.safecarealert.dashboard.SubscriptionRequest;

/**
 * MonitorContext
 * --------------
 * Holds context information for a connected MONITOR (dashboard user).
 * <p>
 * Unlike devices, monitors do NOT belong to a specific group.
 * They can subscribe to one or more workplaces or groups dynamically.
 * <p>
 * The activeSubscription is set when the monitor sends a SubscriptionRequest.
 */
public record MonitorContext(
        String tenantId,
        String monitorId,
        SubscriptionRequest activeSubscription // The current active subscription of this monitor. Can be null if the monitor has connected but not yet subscribed.
) implements ConnectionContext {

    @Override
    public String clientId() {
        return monitorId;
    }

    @Override
    public ClientType clientType() {
        return ClientType.MONITOR;
    }
}
