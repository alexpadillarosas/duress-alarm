package com.safecarealert.alerts.registry;

import com.safecarealert.alerts.GroupKey;
import com.safecarealert.alerts.GroupState;
import com.safecarealert.messages.DeviceMessage;
import com.safecarealert.messages.MessageType;
import com.safecarealert.utils.JsonService;
import com.safecarealert.websocket.AlertSocket;
import io.quarkus.websockets.next.OpenConnections;
import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GroupRegistry
 * -------------
 * Runtime registry of:
 *   • Connected devices per group
 *   • Active alerts per group
 *   • Observer (monitor) subscriptions
 *
 * Responsibilities:
 *   • Track group membership
 *   • Broadcast messages to group members
 *   • Notify observers (delegated to ObserverRegistry)
 *   • Provide active state for hydration
 *
 * This registry is in-memory and single-node only.
 */
@ApplicationScoped
public class GroupRegistry {

    private final Map<GroupKey, Set<String>> groupConnectionIds = new ConcurrentHashMap<>();
    private final Map<GroupKey, GroupState> groupStates = new ConcurrentHashMap<>();

    @Inject Logger log;
    @Inject JsonService jsonService;
    @Inject OpenConnections openConnections;
    @Inject ObserverRegistry observerRegistry;

    // -------------------------------------------------------------------------
    // Group & State Management
    // -------------------------------------------------------------------------

    public GroupState getOrCreateState(GroupKey key) {
        return groupStates.computeIfAbsent(key, k -> new GroupState());
    }

    public GroupState getState(GroupKey key) {
        return groupStates.get(key);
    }

    public Map<GroupKey, GroupState> getGroupStates() {
        return Collections.unmodifiableMap(this.groupStates);
    }

    // -------------------------------------------------------------------------
    // Observer Delegation
    // -------------------------------------------------------------------------

    public void broadcastToObservers(GroupKey key, DeviceMessage msg) {
        observerRegistry.broadcastToObservers(key, msg);
    }

    // -------------------------------------------------------------------------
    // Device Registration
    // -------------------------------------------------------------------------

    public void registerDevice(GroupKey key, String deviceId, WebSocketConnection connection) {
        groupConnectionIds
                .computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet())
                .add(connection.id());
    }

    public void unregisterDevice(GroupKey key, WebSocketConnection connection) {
        Set<String> ids = groupConnectionIds.get(key);

        if (ids != null) {
            ids.remove(connection.id());

            if (ids.isEmpty()) {
                groupConnectionIds.remove(key);
                groupStates.remove(key);
                log.infov("🧹 Group cleared: {0}", jsonService.toJson(key));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Broadcast Logic
    // -------------------------------------------------------------------------

    /**
     * Broadcasts a message to all active connections in the group,
     * except the sender (if provided).
     */
    public void broadcast(GroupKey key, DeviceMessage deviceMessage, WebSocketConnection sender) {

        Set<String> connectionIds = groupConnectionIds.get(key);

        if (connectionIds == null || connectionIds.isEmpty()) {
            log.infov("📭 Broadcast aborted: No active connections for group={0}, unsent message={1}",
                    key, deviceMessage);
            return;
        }

        log.infov("📡 Broadcasting to group={0}, connections={1}", key, connectionIds);

        String jsonPayload = jsonService.toJson(deviceMessage);
        String senderId = sender != null ? sender.id() : null;

        for (String id : connectionIds) {

            if (id.equals(senderId)) {
                log.tracev("↩️ Skipping sender connectionId={0}, message={1}", senderId, deviceMessage);
                continue;
            }

            openConnections.findByConnectionId(id).ifPresentOrElse(conn -> {

                conn.sendText(jsonPayload).subscribe().with(
                        success -> log.infov("⬅️ MESSAGE_SENT: type={0}, toDevice={1}, connectionId={2}, data={3}",
                                deviceMessage.messageType(),
                                conn.userData().get(AlertSocket.DEVICE_ID),
                                id,
                                deviceMessage),

                        failure -> log.errorv("❌ MESSAGE_SEND_FAILED: type={0}, toDevice={1}, connectionId={2}, error={3}",
                                deviceMessage.messageType(),
                                conn.userData().get(AlertSocket.DEVICE_ID),
                                id,
                                failure.getMessage())
                );

            }, () -> {
                log.warnv("⚠️ Stale connectionId={0} found in registry but not in OpenConnections — cleaning up", id);
                connectionIds.remove(id);
            });
        }
    }

    public void broadcast(GroupKey key, DeviceMessage deviceMessage) {
        broadcast(key, deviceMessage, null);
    }

    // -------------------------------------------------------------------------
    // Misc
    // -------------------------------------------------------------------------

    public int getConnectedDeviceCount(GroupKey key) {
        GroupState state = groupStates.get(key);
        return state == null ? 0 : state.connectedDeviceIds.size();
    }

    public void clearAll() {
        groupConnectionIds.clear();
        groupStates.clear();
        log.info("♻️ REGISTRY_RESET: All groups and states wiped.");
    }

    // -------------------------------------------------------------------------
    // Shutdown Handling
    // -------------------------------------------------------------------------

    /**
     * Called by ShutdownManager.
     * Notifies all connected devices of server shutdown.
     */
    public void prepareForShutdown() {
        log.info("📢 GroupRegistry: notifying all connected devices of shutdown...");

        DeviceMessage shutdownMsg = new DeviceMessage(
                MessageType.SERVER_RESTART,
                Map.of(
                        "reason", MessageType.SERVER_RESTART.name(),
                        "message", "The alert system is restarting. Please wait for reconnection."
                )
        );

        groupStates.keySet().forEach(groupKey -> broadcast(groupKey, shutdownMsg));

        log.info("✅ GroupRegistry: All devices notified of shutdown.");
    }
}
