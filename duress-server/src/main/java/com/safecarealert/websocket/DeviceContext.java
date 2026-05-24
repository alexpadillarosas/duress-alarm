package com.safecarealert.websocket;

import com.safecarealert.core.ClientType;

/**
 * DeviceContext
 * -------------
 * Holds context information for a connected DEVICE.
 * <p>
 * Every device must belong to exactly one Group within a Workplace.
 * This context is created during the CONNECT handshake in ConnectHandler.
 */
public record DeviceContext(

        String tenantId,
        String workplaceId,
        String groupId,         // required for devices
        String deviceId

) implements ConnectionContext {

    @Override
    public String clientId() {
        return deviceId;
    }

    @Override
    public ClientType clientType() {
        return ClientType.DEVICE;
    }

}
