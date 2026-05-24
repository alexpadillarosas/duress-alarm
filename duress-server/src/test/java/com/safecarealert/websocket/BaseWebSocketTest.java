package com.safecarealert.websocket;

import com.safecarealert.alerts.registry.GroupRegistry;
import com.safecarealert.authentication.TestTokenGenerator;
import com.safecarealert.core.ClientType;
import com.safecarealert.identity.*;
import com.safecarealert.utils.JsonService;
import io.quarkus.logging.Log;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.BasicWebSocketConnector;
import io.quarkus.websockets.next.CloseReason;
import io.quarkus.websockets.next.WebSocketClientConnection;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * BaseWebSocketTest
 * -----------------
 * Provides:
 *   - Identity registration helpers
 *   - JWT generation
 *   - WebSocket connection helpers
 *   - Automatic CONNECT data sending
 * <p>
 * All tests extend this class.
 */
public abstract class BaseWebSocketTest {

    @ConfigProperty(name = "app.version")
    String appVersion;

//    @Inject
//    @ConfigProperty(name = "jwt.issuer")
//    String issuer;

    @Inject
    protected JsonService jsonService;

    @TestHTTPResource
    protected URI baseUri;

    @Inject
    protected GroupRegistry groupRegistry;

    @Inject
    IdentityService identityService;

    @Inject
    TestTokenGenerator testTokenGenerator;

//    @Inject
//    IdentityTestHelper identityTestHelper;

    // -------------------------------------------------------------------------
    // TEST LIFECYCLE
    // -------------------------------------------------------------------------

    @BeforeEach
    void init() {
        groupRegistry.clearAll();
//        testIdentityService.clear();
        identityService.clear();
    }

    @AfterEach
    void end() {
        // no-op
    }

    // -------------------------------------------------------------------------
    // PATH BUILDERS
    // -------------------------------------------------------------------------

    /**
     * Builds the WebSocket URI for DEVICE clients.
     * No query params. No path params.
     * <p>
     * Example:
     *   ws://localhost:8081/ws/v1/alerts
     */
    protected String getDeviceWsPath() {
        return UriBuilder.fromUri(baseUri)
                .scheme(baseUri.getScheme().equals("https") ? "wss" : "ws")
                .path("/ws/v1/alerts")
                .build()
                .toString();
    }

    /**
     * Builds the WebSocket URI for MONITOR clients.
     */
    protected String getMonitorWsPath() {
        return UriBuilder.fromUri(baseUri)
                .scheme(baseUri.getScheme().equals("https") ? "wss" : "ws")
                .path("/ws/v1/monitor")
                .build()
                .toString();
    }

    // -------------------------------------------------------------------------
    // DEVICE CONNECTION
    // -------------------------------------------------------------------------

    /**
     * Connects a DEVICE to /ws/v1/alerts and sends the CONNECT data.
     */
    public TestDevice connectDevice(String tenantUUID,
                                    String workplaceUUID,
                                    String groupUUID,
                                    String subjectUUID) {

        LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
        AtomicReference<CloseReason> closeRef = new AtomicReference<>();

        String wsPath = getDeviceWsPath();
        String jwt = testTokenGenerator.tokenFor(tenantUUID, subjectUUID);

        BasicWebSocketConnector client = BasicWebSocketConnector.create();

        WebSocketClientConnection connection = client
                .baseUri(wsPath)
                .addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .addHeader("X-Subject-Id", subjectUUID)
                .addHeader("X-Tenant-Id", tenantUUID)
                .onTextMessage((c, msg) -> {
                    queue.offer(msg);
                    Log.tracev("DEVICE[{0}] Received: {1}", subjectUUID, msg);
                })
                .onClose((c, reason) -> closeRef.set(reason))
                .connectAndAwait();

        Map<String, Object> payload = Map.of(
                "clientType", ClientType.DEVICE.name(),
                "subjectId", subjectUUID,
                "tenantId", tenantUUID,
                "workplaceId", workplaceUUID,
                "groupId", groupUUID
        );

        Map<String, Object> envelope = Map.of(
                "messageType", "CONNECT",
                "data", payload
        );

        connection.sendTextAndAwait(jsonService.toJson(envelope));

        return new TestDevice(subjectUUID, connection, queue, jsonService, closeRef);
    }

    // -------------------------------------------------------------------------
    // MONITOR CONNECTION
    // -------------------------------------------------------------------------
    /**
     * Connects a MONITOR to /ws/v1/monitor
     */
    public TestDevice connectMonitor(String tenantUUID, String subjectUUID) {
        return connectMonitor(tenantUUID, subjectUUID, Set.of());
    }
    /**
     * Connects a MONITOR to /ws/v1/monitor and sends CONNECT data.
     */
    public TestDevice connectMonitor(String tenantUUID, String subjectUUID, Set<String> allowedWorkplaces) {

        LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
        AtomicReference<CloseReason> closeRef = new AtomicReference<>();

        String wsPath = getMonitorWsPath();
        String jwt = testTokenGenerator.tokenFor(tenantUUID, subjectUUID);

        BasicWebSocketConnector client = BasicWebSocketConnector.create();

        WebSocketClientConnection connection = client
                .baseUri(wsPath)
                .addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .addHeader("X-Subject-Id", subjectUUID)
                .addHeader("X-Tenant-Id", tenantUUID)
                .onTextMessage((c, msg) -> queue.offer(msg))
                .onClose((c, reason) -> closeRef.set(reason))
                .connectAndAwait();

        Map<String, Object> payload = Map.of(
                "clientType", ClientType.MONITOR.name(),
                "subjectId", subjectUUID,
                "tenantId", tenantUUID,
                "allowedWorkplaces", allowedWorkplaces
        );

        Map<String, Object> envelope = Map.of(
                "messageType", "CONNECT",
                "data", payload
        );

        connection.sendTextAndAwait(jsonService.toJson(envelope));


        return new TestDevice(subjectUUID, connection, queue, jsonService, closeRef);
    }

    // -------------------------------------------------------------------------
    // JWT BUILDER
    // -------------------------------------------------------------------------

//    /**
//     * Builds a JWT for the given identity.
//     */
//    private String tokenFor(String tenantUUID, String subjectUUID) {
//
//        IdentityRecord rec = identityService.lookup(subjectUUID, tenantUUID);
//        if (rec == null) {
//            throw new IllegalStateException("Identity not registered: " + subjectUUID);
//        }
//
//        var jwt = Jwt.issuer(issuer)
//                .subject(rec.subjectUUID())
//                .upn(rec.subjectUUID())
//                .groups(rec.roles())
//                .issuedAt(Instant.now())
//                .expiresIn(Duration.ofHours(24));
//
//        if (rec.tenantUUID() != null) {
//            jwt = jwt.claim(Claim.TENANT_UUID.name(), rec.tenantUUID());
//        }
//
//        if (rec instanceof DeviceIdentity device) {
//            jwt = jwt.claim(Claim.WORKPLACE_UUID.name(), device.workplaceUUID());
//            jwt = jwt.claim(Claim.GROUP_UUID.name(), device.groupUUID());
//            jwt = jwt.claim(Claim.LICENCE_STATUS.name(), device.licenceStatus().name());
//        }
//
//        if (rec instanceof MonitorIdentity monitor) {
//            jwt = jwt.claim(Claim.ALLOWED_WORKPLACES.name(), monitor.allowedWorkplaces());
//        }
//
//        return jwt.sign();
//    }

    // -------------------------------------------------------------------------
    // IDENTITY REGISTRATION
    // -------------------------------------------------------------------------

    /**
     * Registers an identity for testing.
     */
    protected void registerIdentity(String tenantUUID,
                                    String workplaceUUID,
                                    String groupUUID,
                                    String subjectUUID,
                                    Set<String> roles,
                                    Set<String> allowedWorkplaces,
                                    LicenceStatus licenceStatus) {

        IdentityRecord record;

        if (roles.contains(Role.DEVICE.name())) {
            record = new DeviceIdentity(
                    subjectUUID,
                    tenantUUID,
                    workplaceUUID,
                    groupUUID,
                    roles,
                    licenceStatus
            );

        } else if (roles.contains(Role.MONITOR.name())) {

            if (tenantUUID == null || tenantUUID.isBlank()) {
                throw new IllegalStateException("Monitor must have tenantId");
            }

            if (allowedWorkplaces == null || allowedWorkplaces.isEmpty()) {
                throw new IllegalStateException(
                        "Monitor must have at least one allowed workplace (test setup error)"
                );
            }

            record = new MonitorIdentity(
                    subjectUUID,
                    tenantUUID,
                    roles,
                    allowedWorkplaces
            );

        } else if (roles.contains(Role.ADMIN.name())) {
            record = new AdminIdentity(subjectUUID, roles);

        } else if (roles.contains(Role.SUPPORT.name())) {
            record = new SupportIdentity(subjectUUID, tenantUUID, roles);

        } else {
            throw new IllegalStateException("Unknown identity set: " + roles);
        }

//        testIdentityService.register(record);
        identityService.register(record);
    }
}
