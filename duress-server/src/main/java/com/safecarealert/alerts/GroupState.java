package com.safecarealert.alerts;

import com.safecarealert.messages.DeviceMessage;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/***
 *  Class used to know the connected devices in the group
 *  Also keeps the id of the device that triggered the alert alarm
 */
public class GroupState {
    /**
     * Keep the state (DeviceMessage information) for all active alarms
     */
    public Map<String, DeviceMessage> activeAlarms = new ConcurrentHashMap<>();
    /**
     * Keep a list of connected devicesIds
     */
    public Set<String> connectedDeviceIds = ConcurrentHashMap.newKeySet();

}

