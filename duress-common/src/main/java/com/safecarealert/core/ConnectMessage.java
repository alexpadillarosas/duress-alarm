package com.safecarealert.core;

import java.util.Set;

/**
 * ConnectMessage
 * --------------
 * This is the FIRST message every WebSocket client must send after the
 * WebSocket upgrade succeeds.
 *
 * It replaces all query parameters and path parameters.
 *
 * DEVICE example:
 * {
 *   "type": "CONNECT",
 *   "clientType": "DEVICE",
 *   "subjectId": "DEV-0010",
 *   "tenantId": "T1",
 *   "workplaceId": "W1",
 *   "groupId": "G1"
 * }
 * <p>
 * MONITOR example:
 * {
 *   "type": "CONNECT",
 *   "clientType": "MONITOR",
 *   "allowedWorkplaces": ["W1", "W2"]
 * }
 */
public record ConnectMessage(
        String type,            // always "CONNECT"
        ClientType clientType,  // DEVICE or MONITOR
        String subjectId,       // required for DEVICE, required for MONITOR
        String tenantId,        // required for DEVICE, optional for MONITOR
        String workplaceId,     // required for DEVICE
        String groupId,         // required for DEVICE
        Set<String> allowedWorkplaces // for MONITOR only
) {}
