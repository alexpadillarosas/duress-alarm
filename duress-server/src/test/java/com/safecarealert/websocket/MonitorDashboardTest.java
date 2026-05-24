package com.safecarealert.websocket;


import com.safecarealert.dashboard.DeviceStatus;
import com.safecarealert.dashboard.SubscriptionAction;
import com.safecarealert.dashboard.SubscriptionRequest;
import com.safecarealert.identity.LicenceStatus;
import com.safecarealert.identity.Role;
import com.safecarealert.messages.MessageType;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * MonitorDashboardTest
 * --------------------
 * Tests for MONITOR behavior on /ws/v1/monitor endpoint.
 * <p>
 * This test class focuses on:
 *   - Monitor subscriptions (workplace & group level)
 *   - Receiving alerts from subscribed scopes
 *   - Hydration on connect
 *   - Tenant isolation
 */
@QuarkusTest
public class MonitorDashboardTest extends BaseWebSocketTest {

    @Test
    public void testMultiWorkplaceFiltering() {
        // Register monitor with allowed workplaces W1 and W2
        registerIdentity("T1", null, null, "MONITOR-10", Set.of(Role.MONITOR.name()), Set.of("W1", "W2"), LicenceStatus.NONE);

        // Register devices
        registerIdentity("T1", "W1", "G1", "DEV-W1", Set.of(Role.DEVICE.name()), null, LicenceStatus.ACTIVE);
        registerIdentity("T1", "W2", "G1", "DEV-W2", Set.of(Role.DEVICE.name()), null, LicenceStatus.ACTIVE);
        registerIdentity("T1", "W3", "G1", "DEV-W3", Set.of(Role.DEVICE.name()), null, LicenceStatus.ACTIVE);

        // 1. Connect monitor
        TestDevice monitor = connectMonitor("T1", "MONITOR-10");

        // 2. Subscribe to W1 and W2
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(
                SubscriptionAction.SUBSCRIBE,
                List.of(
                        new SubscriptionRequest.MonitorScope("T1", "W1", "*"),
                        new SubscriptionRequest.MonitorScope("T1", "W2", "*")
                )
        );
        monitor.connection().sendTextAndAwait(jsonService.toJson(subscriptionRequest));

        // 3. Connect devices
        TestDevice deviceW1 = connectDevice("T1", "W1", "G1", "DEV-W1");
        TestDevice deviceW2 = connectDevice("T1", "W2", "G1", "DEV-W2");
        TestDevice deviceW3 = connectDevice("T1", "W3", "G1", "DEV-W3");

        deviceW1.waitForMessage(MessageType.DEVICE_CONNECTED);
        deviceW2.waitForMessage(MessageType.DEVICE_CONNECTED);
        deviceW3.waitForMessage(MessageType.DEVICE_CONNECTED);

        // 4. Trigger alert in W3 → monitor must NOT see it
        deviceW3.sendStartAlertToGroup("Room 303", "Dr. No", "Ignored Alert");
        deviceW3.waitForMessage(MessageType.MESSAGE_ACK);

        // 5. Trigger alert in W1 → monitor MUST see it
        deviceW1.sendStartAlertToGroup("Room 101", "Dr. Yes", "Critical Alert");
        deviceW1.waitForMessage(MessageType.MESSAGE_ACK);

        ClientDeviceMessage received = monitor.waitForMessage(MessageType.ALERT_START);

        Assertions.assertNotNull(received);
        Assertions.assertEquals("DEV-W1", received.data().get("alertOwner"));
        Assertions.assertEquals("Room 101", received.data().get("location"));

        // Ensure monitor did NOT receive W3 alert
        Assertions.assertNotEquals("DEV-W3", received.data().get("alertOwner"));

        monitor.close();
        deviceW1.close();
        deviceW2.close();
        deviceW3.close();
    }

    @Test
    public void testMonitorReceivesOnlineOfflineStatus() {
        // Register monitor
        registerIdentity("T1", null, null, "MONITOR-20", Set.of(Role.MONITOR.name()), Set.of("W1"), LicenceStatus.NONE);

        // Register device
        registerIdentity("T1", "W1", "G1", "DEV-0010", Set.of(Role.DEVICE.name()), null, LicenceStatus.ACTIVE);

        // Connect monitor
        TestDevice monitor = connectMonitor("T1", "MONITOR-20");

        // Subscribe to T1/W1/*
        SubscriptionRequest sub = new SubscriptionRequest(
                SubscriptionAction.SUBSCRIBE,
                List.of(new SubscriptionRequest.MonitorScope("T1", "W1", "*"))
        );
        monitor.connection().sendTextAndAwait(jsonService.toJson(sub));

        // Connect device
        TestDevice device = connectDevice("T1", "W1", "G1", "DEV-0010");

        ClientDeviceMessage onlineMsg = monitor.waitForMessage(MessageType.DEVICE_CONNECTED);
        Assertions.assertEquals(DeviceStatus.ONLINE.name(), onlineMsg.data().get("status"));
//        Assertions.assertEquals("W1", onlineMsg.data().get("workplaceId"));

        // Disconnect device
        device.close();

        ClientDeviceMessage offlineMsg = monitor.waitForMessage(MessageType.DEVICE_DISCONNECTED);

        Assertions.assertEquals("DEV-0010", offlineMsg.data().get("deviceId"));
        Assertions.assertEquals(DeviceStatus.OFFLINE.name(), offlineMsg.data().get("status"));

        String lastSeen = offlineMsg.payload().getString("lastSeen");
        Assertions.assertNotNull(lastSeen);
        Assertions.assertTrue(lastSeen.matches("\\d{4}-\\d{2}-\\d{2}T.*Z"));

        Assertions.assertNotNull(offlineMsg.data().get("reason"));

        monitor.close();
    }

    @Test
    public void testTenantIsolationForMonitor() {
        registerIdentity("T1", null, null, "MONITOR-X", Set.of(Role.MONITOR.name()), Set.of("W1"), LicenceStatus.NONE);

        registerIdentity("T1", "W1", "G1", "DEV-T1", Set.of(Role.DEVICE.name()), null, LicenceStatus.ACTIVE);
        registerIdentity("T2", "W1", "G1", "DEV-T2", Set.of(Role.DEVICE.name()), null, LicenceStatus.ACTIVE);

        TestDevice monitor = connectMonitor("T1", "MONITOR-X");

        SubscriptionRequest sub = new SubscriptionRequest(
                SubscriptionAction.SUBSCRIBE,
                List.of(new SubscriptionRequest.MonitorScope("T1", "W1", "*"))
        );
        monitor.connection().sendTextAndAwait(jsonService.toJson(sub));

        TestDevice devT1 = connectDevice("T1", "W1", "G1", "DEV-T1");
        TestDevice devT2 = connectDevice("T2", "W1", "G1", "DEV-T2");

        devT1.waitForMessage(MessageType.DEVICE_CONNECTED);
        devT2.waitForMessage(MessageType.DEVICE_CONNECTED);

        devT2.sendStartAlertToGroup("Room X", "Dr. Wrong", "Should not be seen");
        devT2.waitForMessage(MessageType.MESSAGE_ACK);

        devT1.sendStartAlertToGroup("Room Y", "Dr. Right", "Should be seen");
        devT1.waitForMessage(MessageType.MESSAGE_ACK);

        ClientDeviceMessage msg = monitor.waitForMessage(MessageType.ALERT_START);

        Assertions.assertEquals("DEV-T1", msg.data().get("alertOwner"));
        Assertions.assertNotEquals("DEV-T2", msg.data().get("alertOwner"));

        monitor.close();
        devT1.close();
        devT2.close();
    }

    @Test
    public void testMonitorUnsubscribeStopsEvents() {
        registerIdentity("T1", null, null, "MONITOR-U", Set.of(Role.MONITOR.name()), Set.of("W1"), LicenceStatus.NONE);

        registerIdentity("T1", "W1", "G1", "DEV-100", Set.of(Role.DEVICE.name()), null, LicenceStatus.ACTIVE);

        TestDevice monitor = connectMonitor("T1", "MONITOR-U");

        SubscriptionRequest sub = new SubscriptionRequest(
                SubscriptionAction.SUBSCRIBE,
                List.of(new SubscriptionRequest.MonitorScope("T1", "W1", "*"))
        );
        monitor.connection().sendTextAndAwait(jsonService.toJson(sub));

        TestDevice device = connectDevice("T1", "W1", "G1", "DEV-100");
        device.waitForMessage(MessageType.DEVICE_CONNECTED);

        // Unsubscribe
        SubscriptionRequest unsub = new SubscriptionRequest(
                SubscriptionAction.UNSUBSCRIBE,
                List.of()
        );
        monitor.connection().sendTextAndAwait(jsonService.toJson(unsub));

        // Trigger alert
        device.sendStartAlertToGroup("Room 1", "Dr. No", "Ignored");
        device.waitForMessage(MessageType.MESSAGE_ACK);

        // Monitor must NOT receive anything
        Assertions.assertThrows(
                AssertionError.class,
                () -> monitor.waitForMessage(MessageType.ALERT_START)
        );

        monitor.close();
        device.close();
    }

    @Test
    public void testMonitorHydrationOnSubscribe() {
        registerIdentity("T1", null, null, "MONITOR-H", Set.of(Role.MONITOR.name()), Set.of("W1"), LicenceStatus.NONE);

        registerIdentity("T1", "W1", "G1", "DEV-H1", Set.of(Role.DEVICE.name()), null, LicenceStatus.ACTIVE);

        TestDevice dev = connectDevice("T1", "W1", "G1", "DEV-H1");
        dev.waitForMessage(MessageType.DEVICE_CONNECTED);

        dev.sendStartAlertToGroup("Room 9", "Dr. Hydrate", "Help");
        dev.waitForMessage(MessageType.MESSAGE_ACK);

        TestDevice monitor = connectMonitor("T1", "MONITOR-H");

        SubscriptionRequest sub = new SubscriptionRequest(
                SubscriptionAction.SUBSCRIBE,
                List.of(new SubscriptionRequest.MonitorScope("T1", "W1", "*"))
        );
        monitor.connection().sendTextAndAwait(jsonService.toJson(sub));

        ClientDeviceMessage hydration = monitor.waitForMessage(MessageType.DEVICE_CONNECTED);

        Assertions.assertTrue(Objects.requireNonNull(hydration.payload().getString("activeAlarms")).contains("DEV-H1"));

        monitor.close();
        dev.close();
    }


}
