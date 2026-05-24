package com.safecarealert.filter;

import com.safecarealert.authentication.IdentityTestHelper;
import com.safecarealert.authentication.TestTokenGenerator;
import com.safecarealert.identity.Role;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.mutiny.core.Vertx;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
//@TestProfile(RateLimitTestProfile.class)
class WebSocketValidationFilterTest {

    @Inject
    IdentityTestHelper id;

    @Inject
    TestTokenGenerator testTokenGenerator;
//    @Inject
//    TokenGeneratorResource tokenGeneratorResource;

    @TestHTTPResource("/ws/v1/monitor")
    URI monitorUri;

    @Inject
    Vertx vertx;

    @BeforeEach
    void setup() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        id.clear();
    }

//    // Helper: generate a real JWT using your own system
//    private String jwt(String tenant, String subject) {
//        return tokenGeneratorResource
//                .generate(tenant, subject)
//                .readEntity(TokenGeneratorResource.TokenResponse.class)
//                .token();
//    }

    // -------------------------------------------------------------------------
    // 1. Valid DEVICE request
    // -------------------------------------------------------------------------
    @Test
    void validDeviceRequestPasses() {
        id.identity("DEV-1")
                .tenant("T1")
                .workplace("W1")
                .group("G1")
                .roles(Role.DEVICE.name())
                .register();

        String token = testTokenGenerator.tokenFor("T1", "DEV-1");

        given()
                .header("Upgrade", "websocket")
                .header("Authorization", "Bearer " + token)
                .header("X-Subject-Id", "DEV-1")
                .header("X-Tenant-Id", "T1")
                .when()
                .get("/ws/v1/alerts")
                .then()
                .statusCode(anyOf(is(101), is(404))); // filter passed
    }

    // -------------------------------------------------------------------------
    // 2. Admin may omit X-Tenant-Id
    // -------------------------------------------------------------------------
    @Test
    void adminMayOmitTenantId() {
        id.identity("ADMIN-1")
                .roles(Role.ADMIN.name())
                .register();

        String token = testTokenGenerator.tokenFor(null, "ADMIN-1");
        //  the admin is allowed to omit the tenant ID.
        //  The security filter proudly logs a success:🛡️ WebSocketValidationFilter PASSED: path=/ws/v1/monitor
        //  Because it passes the validation filter, the request is allowed to proceed forward to open the actual socket channel at /ws/v1/monitor.
        //  This is where the framework configuration trips over RestAssured (it cannot upgrade) so we use Vertx
        WebSocketConnectOptions options = new WebSocketConnectOptions()
                .setHost(monitorUri.getHost())
                .setPort(monitorUri.getPort())
                .setURI(monitorUri.getPath())
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("X-Subject-Id", "ADMIN-1");

        CompletableFuture<Boolean> handshakeSuccess = new CompletableFuture<>();

        WebSocketClient wsClient = vertx.getDelegate().createWebSocketClient();

        wsClient.connect(options)
                .onSuccess(socket -> {
                    handshakeSuccess.complete(true); // Handshake upgrade passed flawlessly!
                    socket.close();                 // Clean up connection immediately
                })
                .onFailure(handshakeSuccess::completeExceptionally);

        Assertions.assertDoesNotThrow(() -> {
            handshakeSuccess.get(5, TimeUnit.SECONDS);
        }, "Admin was blocked from the monitor socket channel!");

        /*
        given()
                .header("Upgrade", "websocket")
                .header("Connection", "Upgrade")
                .header("Authorization", "Bearer " + token)
                .header("X-Subject-Id", "ADMIN-1")
                .when()
                .get("/ws/v1/monitor")
                .then()
                .statusCode(anyOf(is(200)));

         */
    }

    // -------------------------------------------------------------------------
    // 2. Admin may omit X-Tenant-Id
    // -------------------------------------------------------------------------
    @Test
    void notAdminCannotOmitTenantId() {
        id.identity("DEV-100")
                .tenant("T100")
                .workplace("W100")
                .group("G100")
                .roles(Role.DEVICE.name())
                .register();

        String token = testTokenGenerator.tokenFor(null, "DEV-100");

        given()
                .header("Upgrade", "websocket")
                .header("Authorization", "Bearer " + token)
                .header("X-Subject-Id", "DEV-100")
                .when()
                .get("/ws/v1/alerts")
                .then()
                .statusCode(is(400));
//                .statusCode(anyOf(is(101), is(404)));
    }

    // -------------------------------------------------------------------------
    // 3. Missing Authorization header
    // -------------------------------------------------------------------------
    @Test
    void missingAuthorizationFails() {
        given()
                .header("Upgrade", "websocket")
                .header("X-Subject-Id", "DEV-1")
                .header("X-Tenant-Id", "T1")
                .when()
                .get("/ws/v1/alerts")
                .then()
                .statusCode(401)
                .body(containsString("Missing Authorization Bearer token"));
    }

    // -------------------------------------------------------------------------
    // 4. Missing X-Subject-Id
    // -------------------------------------------------------------------------
    @Test
    void missingSubjectIdFails() {
        id.identity("DEV-1")
                .tenant("T1")
                .workplace("W1")
                .group("G1")
                .roles(Role.DEVICE.name())
                .register();

        String token = testTokenGenerator.tokenFor("T1", "DEV-1");

        given()
                .header("Upgrade", "websocket")
                .header("Connection", "Upgrade")
                .header("Authorization", "Bearer " + token)
                .header("X-Tenant-Id", "T1")
                .when()
                .get("/ws/v1/alerts")
                .then()
                .statusCode(400)
                .body(containsString("X-Subject-Id header is required"));
    }

    // -------------------------------------------------------------------------
    // 5. Non-admin missing X-Tenant-Id
    // -------------------------------------------------------------------------
    @Test
    void nonAdminMissingTenantIdHeaderFails() {
        // Register a complete DEVICE identity, this one has tenant but the token may be stolen so we need to
        // enforce that the client passes the subject and tenant id as headers, we will check it matches.
        id.identity("DEV-1")
                .tenant("T1")
                .workplace("W1")
                .group("G1")
                .roles(Role.DEVICE.name())
                .register();

        // Generate a real JWT for DEV-1
        String token = testTokenGenerator.tokenFor("T1", "DEV-1");

        // Omit X-Tenant-Id on purpose (this is what must fail)
        given()
                .header("Upgrade", "websocket")
//                .header("Connection", "Upgrade")
                .header("Authorization", "Bearer " + token)
                .header("X-Subject-Id", "DEV-1")
                .when()
                .get("/ws/v1/alerts")
                .then()
                .statusCode(400)
                .body(containsString("X-Tenant-Id / tenantUUID claim is required for non-admin users"));
    }


    // -------------------------------------------------------------------------
    // 6. Invalid JWT token
    // -------------------------------------------------------------------------
//    @Test
//    void invalidJwtFails() {
//        given()
//                .header("Upgrade", "websocket")
//                .header("Authorization", "Bearer not-a-jwt")
//                .header("X-Subject-Id", "DEV-1")
//                .header("X-Tenant-Id", "T1")
//                .when()
//                .get("/ws/v1/alerts")
//                .then()
//                .statusCode(401)
//                .body(containsString("Invalid JWT token"));
//    }

    // -------------------------------------------------------------------------
    // 7. Invalid path structure
    // -------------------------------------------------------------------------
    @Test
    void invalidPathStructureFails() {
        id.identity("DEV-1")
                .tenant("T1")
                .workplace("W1")
                .group("G1")
                .roles(Role.DEVICE.name())
                .register();

        String token = testTokenGenerator.tokenFor("T1", "DEV-1");

        given()
                .header("Upgrade", "websocket")
                .header("Authorization", "Bearer " + token)
                .header("X-Subject-Id", "DEV-1")
                .header("X-Tenant-Id", "T1")
                .when()
                .get("/ws/v1")
                .then()
                .statusCode(400)
                .body(containsString("Invalid path structure"));
    }

    // -------------------------------------------------------------------------
    // 8. Unknown endpoint
    // -------------------------------------------------------------------------
    @Test
    void unknownEndpointFails() {
        id.identity("DEV-1")
                .tenant("T1")
                .workplace("W1")
                .group("G1")
                .roles(Role.DEVICE.name())
                .register();

        String token = testTokenGenerator.tokenFor("T1", "DEV-1");

        given()
                .header("Upgrade", "websocket")
                .header("Authorization", "Bearer " + token)
                .header("X-Subject-Id", "DEV-1")
                .header("X-Tenant-Id", "T1")
                .when()
                .get("/ws/v1/unknown")
                .then()
                .statusCode(400)
                .body(containsString("Unknown endpoint"));
    }

    // -------------------------------------------------------------------------
    // 9. Rate limiting
    // -------------------------------------------------------------------------
    /*
    @Test
    void rateLimitExceeded() {
        id.identity("DEV-1")
                .tenant("T1")
                .workplace("W1")
                .group("G1")
                .roles(Role.DEVICE.name())
                .register();

        String token = jwt("T1", "DEV-1");

        // First request passes
        given()
                .header("Upgrade", "websocket")
                .header("Authorization", "Bearer " + token)
                .header("X-Subject-Id", "DEV-1")
                .header("X-Tenant-Id", "T1")
                .when()
                .get("/ws/v1/alerts")
                .then()
                .statusCode(anyOf(is(101), is(404)));

        // Second request is rate-limited
        given()
                .header("Upgrade", "websocket")
                .header("Authorization", "Bearer " + token)
                .header("X-Subject-Id", "DEV-1")
                .header("X-Tenant-Id", "T1")
                .when()
                .get("/ws/v1/alerts")
                .then()
                .statusCode(429)
                .body(containsString("Too many requests"));
    }
    */




}
