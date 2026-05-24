package com.safecarealert.websocket;

import com.safecarealert.core.MessageStatus;
import com.safecarealert.core.TerminationReason;
import com.safecarealert.identity.LicenceStatus;
import com.safecarealert.identity.Role;
import com.safecarealert.messages.MessageType;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.websockets.next.CloseReason;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.Set;

/**
 * Tests for DEVICE behavior on /ws/v1/alerts endpoint
 * These tests focus only on devices. Monitor-specific tests are in MonitorDashboardTest
 */

@QuarkusTest
public class AlertSocketTest extends BaseWebSocketTest {

    @Test
    public void testDeviceConnection() {
        registerIdentity("T1", "W1", "G1", "DEV-0000", Set.of(Role.DEVICE.name()), null, LicenceStatus.ACTIVE);
        registerIdentity("T1", "W1", "G1", "DEV-0001", Set.of(Role.DEVICE.name()), null, LicenceStatus.ACTIVE);

        TestDevice device0000 = connectDevice("T1", "W1", "G1", "DEV-0000");
        ClientDeviceMessage msg0 = device0000.waitForMessage(MessageType.DEVICE_CONNECTED);

        Assertions.assertNotNull(msg0);
        Assertions.assertEquals(MessageType.DEVICE_CONNECTED, msg0.messageType());
        Assertions.assertEquals(appVersion, msg0.payload().getString("appVersion"));
        Assertions.assertEquals("1", msg0.payload().getString("activeDevices"));
        Assertions.assertEquals("GP's on Vermont", msg0.payload().getString("organisationName"));

        TestDevice device0001 = connectDevice("T1", "W1", "G1", "DEV-0001");
        ClientDeviceMessage msg1 = device0001.waitForMessage(MessageType.DEVICE_CONNECTED);

        Assertions.assertNotNull(msg1);
        Assertions.assertEquals(MessageType.DEVICE_CONNECTED, msg1.messageType());
        Assertions.assertEquals(appVersion, msg1.payload().getString("appVersion"));
        Assertions.assertEquals("2", msg1.payload().getString("activeDevices"));
        Assertions.assertEquals("GP's on Vermont", msg1.payload().getString("organisationName"));

        device0000.close();
        device0001.close();
    }

    @Test
    public void testAlertAcknowledgement() {
        registerIdentity("T1", "W1", "G1", "DEV-0010", Set.of(Role.DEVICE.name()), null, LicenceStatus.ACTIVE);
        registerIdentity("T1", "W1", "G1", "DEV-0020", Set.of(Role.DEVICE.name()), null, LicenceStatus.ACTIVE);

        TestDevice alertOwner = connectDevice("T1", "W1", "G1", "DEV-0010");
        TestDevice device0020 = connectDevice("T1", "W1", "G1", "DEV-0020");

        alertOwner.clearMessages();
        device0020.clearMessages();

        alertOwner.sendStartAlertToGroup("Room 201", "Dr. Doom", "Assistance needed");

        ClientDeviceMessage broadcast = device0020.waitForMessage(MessageType.ALERT_START);
        String correlationId = broadcast.correlationId();

        Assertions.assertNotNull(correlationId);
        Assertions.assertEquals("Room 201", broadcast.payload().getString("location"));
        Assertions.assertEquals("Dr. Doom", broadcast.payload().getString("person"));
        Assertions.assertEquals("Assistance needed", broadcast.payload().getString("message"));

        ClientDeviceMessage ack = alertOwner.waitForMessage(MessageType.MESSAGE_ACK);

        Assertions.assertEquals("BROADCAST_COMPLETE", ack.payload().getString("status"));
        Assertions.assertEquals(correlationId, ack.correlationId());
        Assertions.assertNotNull(ack.payload().getString("notifiedDevices"));

        alertOwner.close();
        device0020.close();
    }

    @Test
    void testAlertOwnerDoesNotReceiveItsOwnAlert() {
        registerIdentity("T1", "W1", "G1", "DEV-0020", Set.of(Role.DEVICE.name()), null, LicenceStatus.ACTIVE);

        TestDevice alertOwner = connectDevice("T1", "W1", "G1", "DEV-0020");
        alertOwner.waitForMessage(MessageType.DEVICE_CONNECTED);

        alertOwner.sendStartAlertToGroup("Room 201", "Dr. Doom", "Assistance needed");

        ClientDeviceMessage ack = alertOwner.waitForMessage(MessageType.MESSAGE_ACK);

        Assertions.assertEquals(MessageStatus.BROADCAST_COMPLETE.name(), ack.payload().getString("status"));
        Assertions.assertEquals("", ack.payload().getString("notifiedDevices"));

        alertOwner.close();
    }

    @Test
    public void testAlertStartStopCycle() {
        registerIdentity("T1", "W1", "G1", "DEV-0010", Set.of(Role.DEVICE.name()), null, LicenceStatus.ACTIVE);
        registerIdentity("T1", "W1", "G1", "DEV-0020", Set.of(Role.DEVICE.name()), null, LicenceStatus.ACTIVE);

        TestDevice owner = connectDevice("T1", "W1", "G1", "DEV-0010");
        TestDevice responder = connectDevice("T1", "W1", "G1", "DEV-0020");

        owner.clearMessages();
        responder.clearMessages();

        owner.sendStartAlertToGroup("Room 201", "Dr. Doom", "Assistance needed");

        ClientDeviceMessage broadcast = responder.waitForMessage(MessageType.ALERT_START);
        Assertions.assertEquals("DEV-0010", broadcast.payload().getString("alertOwner"));

        ClientDeviceMessage ack = owner.waitForMessage(MessageType.MESSAGE_ACK);
        Assertions.assertEquals(MessageStatus.BROADCAST_COMPLETE.name(), ack.payload().getString("status"));
//        Assertions.assertTrue(ack.data().getString("notifiedDevices").contains(responder.clientId()));
        Assertions.assertTrue(Objects.requireNonNull(ack.payload().getString("notifiedDevices")).contains(responder.clientId()));

        responder.sendStopAlertToGroup(owner.clientId(), broadcast.correlationId());

        ClientDeviceMessage stopNotify = owner.waitForMessage(MessageType.ALERT_STOP);
        Assertions.assertEquals(responder.clientId(), stopNotify.payload().getString("stoppedBy"));

        ClientDeviceMessage stopAck = responder.waitForMessage(MessageType.MESSAGE_ACK);
        Assertions.assertEquals(MessageStatus.STOP_COMPLETE.name(), stopAck.payload().getString("status"));

        owner.close();
        responder.close();
    }

    @Test
    public void testActiveAlertWhenNoClientConnected() {
        registerIdentity("T1", "W1", "G1", "DEV-0010", Set.of(Role.DEVICE.name()), null, LicenceStatus.ACTIVE);

        TestDevice owner = connectDevice("T1", "W1", "G1", "DEV-0010");
        owner.clearMessages();

        owner.sendStartAlertToGroup("Room 201", "Dr. Smith", "Assistance needed");

        ClientDeviceMessage ack = owner.waitForMessage(MessageType.MESSAGE_ACK);
        Assertions.assertEquals(MessageStatus.BROADCAST_COMPLETE.name(), ack.payload().getString("status"));
        Assertions.assertEquals("", ack.payload().getString("notifiedDevices"));

        owner.close();
    }

//    @Test
//    public void testLateJoinerReceivesActiveAlarmsOnConnect() {
//        registerIdentity("T1", "W1", "G1", "DEV-0010", Set.of(Role.DEVICE.name()), null, LicenceStatus.ACTIVE);
//        registerIdentity("T1", "W1", "G1", "DEV-0020", Set.of(Role.DEVICE.name()), null, LicenceStatus.ACTIVE);
//
//        TestDevice owner = connectDevice("T1", "W1", "G1", "DEV-0010");
//        owner.waitForMessage(MessageType.DEVICE_CONNECTED);
//
//        owner.sendStartAlertToGroup("Room 201", "Dr. Williams", "Assistance needed");
//        owner.waitForMessage(MessageType.MESSAGE_ACK);
//
//        TestDevice lateJoiner = connectDevice("T1", "W1", "G1", "DEV-0020");
//        ClientDeviceMessage response = lateJoiner.waitForMessage(MessageType.DEVICE_CONNECTED);
//
////        String json = response.data().getString("activeAlertingDevices");
//        String json = response.data().getString("activeAlertingDevices");
//        Map<String, ClientDeviceMessage> active = jsonService.fromJson(json, new TypeReference<>() {});
//
//        Assertions.assertTrue(active.containsKey(owner.clientId()));
//
//        owner.close();
//        lateJoiner.close();
//    }


    @Test
    public void testLateJoinerReceivesActiveAlertsOnConnect() {

        registerIdentity("T1", "W1", "G1", "DEV-0010", Set.of(Role.DEVICE.name()), null, LicenceStatus.ACTIVE);
        registerIdentity("T1", "W1", "G1", "DEV-0020", Set.of(Role.DEVICE.name()), null, LicenceStatus.ACTIVE);

        // 1️⃣ Owner connects
        TestDevice owner = connectDevice("T1", "W1", "G1", "DEV-0010");
        owner.waitForMessage(MessageType.DEVICE_CONNECTED);

        // 2️⃣ Owner triggers alert
        owner.sendStartAlertToGroup("Room 201", "Dr. Williams", "Assistance needed");
        owner.waitForMessage(MessageType.MESSAGE_ACK);

        // 3️⃣ Late joiner connects
        TestDevice lateJoiner = connectDevice("T1", "W1", "G1", "DEV-0020");

        // 4️⃣ First message must be DEVICE_CONNECTED
        ClientDeviceMessage connected = lateJoiner.waitForMessage(MessageType.DEVICE_CONNECTED);
        Assertions.assertNotNull(connected);

        // 5️⃣ Next message must be ALERT_START replay
        ClientDeviceMessage replayed = lateJoiner.waitForMessage(MessageType.ALERT_START);
        Assertions.assertNotNull(replayed);

        // 6️⃣ Assert replayed alert belongs to owner
        Assertions.assertEquals("DEV-0010", replayed.payload().getString("alertOwner"));

        owner.close();
        lateJoiner.close();
    }



    @Test
    public void testLateJoinerReplyActiveAlarmsOnConnect() {

        registerIdentity("T1", "W1", "G1", "DEV-0010",
                Set.of(Role.DEVICE.name()), null, LicenceStatus.ACTIVE);

        registerIdentity("T1", "W1", "G1", "DEV-0020",
                Set.of(Role.DEVICE.name()), null, LicenceStatus.ACTIVE);

        // Owner connects
        TestDevice owner = connectDevice("T1", "W1", "G1", "DEV-0010");
        owner.waitForMessage(MessageType.DEVICE_CONNECTED);

        // Owner triggers alert
        owner.sendStartAlertToGroup("Room 201", "Dr. Williams", "Assistance needed");
        owner.waitForMessage(MessageType.MESSAGE_ACK);

        // Late joiner connects
        TestDevice lateJoiner = connectDevice("T1", "W1", "G1", "DEV-0020");

        // First message must be DEVICE_CONNECTED
        ClientDeviceMessage connected = lateJoiner.waitForMessage(MessageType.DEVICE_CONNECTED);
        Assertions.assertNotNull(connected);

        // Next message must be the replayed ALERT_START
        ClientDeviceMessage replayed = lateJoiner.waitForMessage(MessageType.ALERT_START);
        Assertions.assertNotNull(replayed);

        // Assert replayed alert belongs to the owner
        Assertions.assertEquals("DEV-0010",
                replayed.payload().getString("alertOwner"));

        // Late joiner stops the alert using the replayed correlationId
        lateJoiner.sendStopAlertToGroup("DEV-0010", replayed.correlationId());

        // Owner receives ALERT_STOP
        ClientDeviceMessage stopNotify = owner.waitForMessage(MessageType.ALERT_STOP);
        Assertions.assertEquals("DEV-0020",
                stopNotify.payload().getString("stoppedBy"));

        // Late joiner receives MESSAGE_ACK with STOP_COMPLETE
        ClientDeviceMessage stopAck = lateJoiner.waitForMessage(MessageType.MESSAGE_ACK);
        Assertions.assertEquals(MessageStatus.STOP_COMPLETE.name(),
                stopAck.payload().getString("status"));

        owner.close();
        lateJoiner.close();
    }


    @Test
    public void testDoubleStartAlertAttemptFromSameDevice() {
        registerIdentity("T1", "W1", "G1", "DEV-0010", Set.of(Role.DEVICE.name()), null, LicenceStatus.ACTIVE);

        TestDevice owner = connectDevice("T1", "W1", "G1", "DEV-0010");
        owner.waitForMessage(MessageType.DEVICE_CONNECTED);

        owner.sendStartAlertToGroup("Room 201", "Dr. Williams", "Assistance needed");
        ClientDeviceMessage ack1 = owner.waitForMessage(MessageType.MESSAGE_ACK);

        Assertions.assertEquals(MessageStatus.BROADCAST_COMPLETE.name(), ack1.payload().getString("status"));

        owner.sendStartAlertToGroup("Room 201", "Dr. Williams", "Assistance needed");
        ClientDeviceMessage ack2 = owner.waitForMessage(MessageType.MESSAGE_ACK);

        Assertions.assertEquals(MessageStatus.ALERT_ALREADY_STARTED.name(), ack2.payload().getString("status"));
        Assertions.assertEquals("", ack2.payload().getString("notifiedDevices"));

        owner.close();
    }

    @Test
    public void testStopAlertWhenAlarmAlreadyStopped() {
        registerIdentity("T1", "W1", "G1", "DEV-0010", Set.of(Role.DEVICE.name()), null, LicenceStatus.ACTIVE);
        registerIdentity("T1", "W1", "G1", "DEV-0020", Set.of(Role.DEVICE.name()), null, LicenceStatus.ACTIVE);
        registerIdentity("T1", "W1", "G1", "DEV-0030", Set.of(Role.DEVICE.name()), null, LicenceStatus.ACTIVE);

        TestDevice owner = connectDevice("T1", "W1", "G1", "DEV-0010");
        TestDevice first = connectDevice("T1", "W1", "G1", "DEV-0020");
        TestDevice second = connectDevice("T1", "W1", "G1", "DEV-0030");

        owner.waitForMessage(MessageType.DEVICE_CONNECTED);
        first.waitForMessage(MessageType.DEVICE_CONNECTED);
        second.waitForMessage(MessageType.DEVICE_CONNECTED);

        owner.sendStartAlertToGroup("Room 201", "Dr. Doom", "Assistance needed");

        ClientDeviceMessage b1 = first.waitForMessage(MessageType.ALERT_START);
        ClientDeviceMessage b2 = second.waitForMessage(MessageType.ALERT_START);

        owner.waitForMessage(MessageType.MESSAGE_ACK);

        first.sendStopAlertToGroup(owner.clientId(), b1.correlationId());

        ClientDeviceMessage stop1 = owner.waitForMessage(MessageType.ALERT_STOP);
        Assertions.assertEquals(first.clientId(), stop1.payload().getString("stoppedBy"));

        ClientDeviceMessage stop2 = second.waitForMessage(MessageType.ALERT_STOP);
        Assertions.assertEquals(first.clientId(), stop2.payload().getString("stoppedBy"));

        ClientDeviceMessage ack1 = first.waitForMessage(MessageType.MESSAGE_ACK);
        Assertions.assertEquals(MessageStatus.STOP_COMPLETE.name(), ack1.payload().getString("status"));

        second.sendStopAlertToGroup(owner.clientId(), b2.correlationId());

        ClientDeviceMessage ack2 = second.waitForMessage(MessageType.MESSAGE_ACK);
        Assertions.assertEquals(MessageStatus.ALERT_ALREADY_STOPPED.name(), ack2.payload().getString("status"));
        Assertions.assertTrue( Objects.requireNonNull(ack2.payload().getString("notifiedDevices")).contains(owner.clientId()));
        Assertions.assertTrue( Objects.requireNonNull(ack2.payload().getString("notifiedDevices")).contains(first.clientId()));

        owner.close();
        first.close();
        second.close();
    }

    @Test
    public void testMalformedJson() {
        registerIdentity("T1", "W1", "G1", "DEV-0099", Set.of(Role.DEVICE.name()), null, LicenceStatus.ACTIVE);

        TestDevice device = connectDevice("T1", "W1", "G1", "DEV-0099");
        device.waitForMessage(MessageType.DEVICE_CONNECTED);

        // Send invalid JSON
        device.connection().sendTextAndAwait("{ this is not valid json ");

        CloseReason reason = device.waitForCloseReason();
        Assertions.assertEquals(TerminationReason.INVALID_JSON.getCode(), reason.getCode());
    }

    @Test
    public void testUnknownMessageType() {
        registerIdentity("T1", "W1", "G1", "DEV-0100", Set.of(Role.DEVICE.name()), null, LicenceStatus.ACTIVE);

        TestDevice device = connectDevice("T1", "W1", "G1", "DEV-0100");
        device.waitForMessage(MessageType.DEVICE_CONNECTED);

        // Unknown message type
        String unknown = """
        {
          "messageType": "ALIEN_SIGNAL",
          "data": { "foo": "bar" }
        }
        """;

        device.connection().sendTextAndAwait(unknown);

        ClientDeviceMessage fault = device.waitForMessage(MessageType.SYSTEM_FAULT);
        Assertions.assertEquals(TerminationReason.UNKNOWN_MESSAGE_TYPE.name(), fault.payload().getString("error"));
        Assertions.assertEquals(TerminationReason.UNKNOWN_MESSAGE_TYPE.getDescription(), fault.payload().getString("details"));

        CloseReason reason = device.waitForCloseReason();
        Assertions.assertEquals(TerminationReason.UNKNOWN_MESSAGE_TYPE.getCode(), reason.getCode());
        Assertions.assertEquals(TerminationReason.UNKNOWN_MESSAGE_TYPE.getDescription(), reason.getMessage());
    }

    @Test
    public void testIndependentAlarmLifeCycle() {
        registerIdentity("T1", "W1", "G1", "DEV-A", Set.of(Role.DEVICE.name()), null, LicenceStatus.ACTIVE);
        registerIdentity("T1", "W1", "G1", "DEV-B", Set.of(Role.DEVICE.name()), null, LicenceStatus.ACTIVE);
        registerIdentity("T1", "W1", "G1", "DEV-C", Set.of(Role.DEVICE.name()), null, LicenceStatus.ACTIVE);

        TestDevice A = connectDevice("T1", "W1", "G1", "DEV-A");
        TestDevice B = connectDevice("T1", "W1", "G1", "DEV-B");
        TestDevice C = connectDevice("T1", "W1", "G1", "DEV-C");

        A.waitForMessage(MessageType.DEVICE_CONNECTED);
        B.waitForMessage(MessageType.DEVICE_CONNECTED);
        C.waitForMessage(MessageType.DEVICE_CONNECTED);

        A.clearMessages();
        B.clearMessages();
        C.clearMessages();

        // A triggers alert #1
        A.sendStartAlertToGroup("Room 1", "Dr. A", "Help A");
        ClientDeviceMessage alert1_B = B.waitForMessage(MessageType.ALERT_START);
        ClientDeviceMessage alert1_C = C.waitForMessage(MessageType.ALERT_START);

        // B triggers alert #2
        B.sendStartAlertToGroup("Room 2", "Dr. B", "Help B");
        ClientDeviceMessage alert2_A = A.waitForMessage(MessageType.ALERT_START);
        ClientDeviceMessage alert2_C = C.waitForMessage(MessageType.ALERT_START);

        // Stop alert #1
        C.sendStopAlertToGroup(A.clientId(), alert1_C.correlationId());
        ClientDeviceMessage stop1_A = A.waitForMessage(MessageType.ALERT_STOP);
        ClientDeviceMessage stop1_B = B.waitForMessage(MessageType.ALERT_STOP);

        // Stop alert #2
        A.sendStopAlertToGroup(B.clientId(), alert2_A.correlationId());
        ClientDeviceMessage stop2_B = B.waitForMessage(MessageType.ALERT_STOP);
        ClientDeviceMessage stop2_C = C.waitForMessage(MessageType.ALERT_STOP);

        A.close();
        B.close();
        C.close();
    }

    @Test
    public void testTripleTriggerSpamProtection() {
        registerIdentity("T1", "W1", "G1", "DEV-SPAM", Set.of(Role.DEVICE.name()), null, LicenceStatus.ACTIVE);

        TestDevice spammer = connectDevice("T1", "W1", "G1", "DEV-SPAM");
        spammer.waitForMessage(MessageType.DEVICE_CONNECTED);

        spammer.clearMessages();

        // First alert → accepted
        spammer.sendStartAlertToGroup("Room X", "Dr. X", "Help");
        ClientDeviceMessage ack1 = spammer.waitForMessage(MessageType.MESSAGE_ACK);
        Assertions.assertEquals(MessageStatus.BROADCAST_COMPLETE.name(), ack1.payload().getString("status"));

        // Second alert → rejected
        spammer.sendStartAlertToGroup("Room X", "Dr. X", "Help");
        ClientDeviceMessage ack2 = spammer.waitForMessage(MessageType.MESSAGE_ACK);
        Assertions.assertEquals(MessageStatus.ALERT_ALREADY_STARTED.name(), ack2.payload().getString("status"));

        // Third alert → rejected
        spammer.sendStartAlertToGroup("Room X", "Dr. X", "Help");
        ClientDeviceMessage ack3 = spammer.waitForMessage(MessageType.MESSAGE_ACK);
        Assertions.assertEquals(MessageStatus.ALERT_ALREADY_STARTED.name(), ack3.payload().getString("status"));

        spammer.close();
    }

    @Test
    public void testAlertOwnerDisconnection() {
        registerIdentity("T1", "W1", "G1", "DEV-OWNER", Set.of(Role.DEVICE.name()), null, LicenceStatus.ACTIVE);
        registerIdentity("T1", "W1", "G1", "DEV-RESP", Set.of(Role.DEVICE.name()), null, LicenceStatus.ACTIVE);

        TestDevice owner = connectDevice("T1", "W1", "G1", "DEV-OWNER");
        TestDevice responder = connectDevice("T1", "W1", "G1", "DEV-RESP");

        owner.waitForMessage(MessageType.DEVICE_CONNECTED);
        responder.waitForMessage(MessageType.DEVICE_CONNECTED);

        owner.clearMessages();
        responder.clearMessages();

        owner.sendStartAlertToGroup("Room 9", "Dr. Z", "Help");

        ClientDeviceMessage broadcast = responder.waitForMessage(MessageType.ALERT_START);
        owner.waitForMessage(MessageType.MESSAGE_ACK);

        // Owner disconnects while alert active
        owner.close();

        // Responder must receive ALERT_STOP
        ClientDeviceMessage stop = responder.waitForMessage(MessageType.ALERT_STOP);
        Assertions.assertEquals("DEV-OWNER", stop.payload().getString("alertOwner"));

        responder.close();
    }


}
