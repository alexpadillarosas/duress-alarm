package com.safecarealert.core;

public final class ConnectValidation {

    private ConnectValidation() {}

    public static boolean isDeviceConnect(ConnectMessage msg) {
        return msg != null
                && "CONNECT".equalsIgnoreCase(msg.type())
                && msg.clientType() == ClientType.DEVICE;
    }

    public static boolean isMonitorConnect(ConnectMessage msg) {
        return msg != null
                && "CONNECT".equalsIgnoreCase(msg.type())
                && msg.clientType() == ClientType.MONITOR;
    }
}
