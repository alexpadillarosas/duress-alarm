package com.safecarealert.websocket;

import com.safecarealert.identity.Claim;
import com.safecarealert.identity.Role;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocketPathValidationFilter
 * -----------------------------
 * PURPOSE
 * -------
 * Fast-fail guard for WebSocket upgrade requests before they hit CDI / business logic.
 * ARCHITECTURAL ROLE: Infrastructure Gatekeeper (L7 Firewall).
 * - Protects CPU from Cryptographic DoS via Rate Limiting.
 * - Validates Protocol and Path before Handshake.
 * - Enforces Mandatory Header Presence.
 * It validates:
 *   - Protocol: only WebSocket upgrade requests are processed.
 *   - Path structure:
 *       /ws/v{n}/alerts
 *       /ws/v{n}/monitor
 *   - Mandatory headers:
 *       Authorization: Bearer <JWT>
 *       X-Subject-Id: always required
 *       X-Tenant-Id:
 *          - required for NON-admin identities
 *          - optional for ADMIN identities
 * <p/>
 * SECURITY NOTE
 * -------------
 * - This filter does NOT perform full authorization.
 * - It only:
 *      • Validates basic URL structure and headers
 *      • Parses JWT to detect ADMIN role for header rules
 * - Full security is still enforced by:
 *      • JWT / SecurityIdentity in the WebSocket endpoints
 *      • @RolesAllowed on the WebSocket classes
 */
@ApplicationScoped
public class WebSocketValidationFilter {

    @Inject Logger log;
    @Inject JWTParser jwtParser;

    @ConfigProperty(name = "app.websocket.base.path", defaultValue = "/ws")
    String wsBasePath; // e.g. "/ws"

    @ConfigProperty(name = "app.security.ratelimit.limit", defaultValue = "500")
    int rateLimit;

    @ConfigProperty(name = "app.security.ratelimit.window-seconds", defaultValue = "1")
    int windowSeconds;

    @ConfigProperty(name = "app.security.ratelimit.enabled", defaultValue = "true")
    boolean rateLimitEnabled;

    // --- Internal State ---
    private Cache<String, AtomicInteger> ipCache;

    @PostConstruct
    void initRateLimiter() {
        // Sliding window: Expire entry X seconds after the last write
        this.ipCache = Caffeine.newBuilder()
                .expireAfterWrite(windowSeconds, TimeUnit.SECONDS)
                .maximumSize(10_000) // Defensive cap against IP spoofing
                .build();

        if (rateLimitEnabled) {
            log.infov("🛡️ Rate Limiter Active: {0} req / {1}s", rateLimit, windowSeconds);
        }
    }

    void register(@Observes StartupEvent ev, Router router) {
        log.infov("🔥 WebSocketPathValidationFilter registered. BasePath: {0}", wsBasePath);
        //TODO: change order to 10100 when Quarkus Automatic JWT validation is activated.
        router.route().order(-100).handler(this::handle);
    }

    private void handle(RoutingContext ctx) {
        // Since Quarkus Automatic token validation is activated, it will execute the validation at order 10_000
        // If the JWT was invalid,a expired or missing, Quarkus already failed the request
        /*
        if ( ctx.user() == null){
            ctx.response().setStatusCode(401).end("Unauthorized: Invalid Token");
        }
        */
        // 1. Get Client IP (X-Forwarded-For Aware)
        String clientIp = getClientIp(ctx);

        // 2. IP-Based Rate Limit (Protect the CPU first)
        if (isRateLimited(clientIp)) {
            log.warnf("🚫 RATE_LIMIT_EXCEEDED: IP=%s", clientIp);
            ctx.response()
                    .setStatusCode(429)
                    .putHeader("Retry-After", String.valueOf(windowSeconds))
                    .end("Too many requests. Please retry in " + windowSeconds + "s.");
            return;
        }

        // 3. Only intercept WebSocket Upgrades
        String upgrade = ctx.request().getHeader("Upgrade");
        if (!"websocket".equalsIgnoreCase(upgrade)) {
            ctx.next();
            return;
        }

        String path = ctx.normalizedPath();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        // 4. Path Validation: /ws/v{n}/endpoint
        if (!path.startsWith(wsBasePath + "/")) {
            fail(ctx, 400, "Invalid WebSocket path scope");
            return;
        }

        String[] parts = path.split("/");
        if (parts.length != 4) {
            fail(ctx, 400, "Invalid path structure. Format: /ws/v{n}/[alerts|monitor]");
            return;
        }

        String version = parts[2]; // v1
        String endpoint = parts[3]; // alerts/monitor

        if (!version.matches("v[0-9]+")) {
            fail(ctx, 400, "Invalid API version format");
            return;
        }

        if (!"alerts".equals(endpoint) && !"monitor".equals(endpoint)) {
            log.warnv("❌ REJECTED: Unknown endpoint: {0}", endpoint);
            fail(ctx, 400, "Unknown endpoint: " + endpoint);
            return;
        }

        // === AUTHENTICATION ===
        String authHeader = ctx.request().getHeader(HttpHeaders.AUTHORIZATION);
        String subjectHeader = ctx.request().getHeader("X-Subject-Id");
        String tenantHeader = ctx.request().getHeader("X-Tenant-Id");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            fail(ctx, 401, "Missing Authorization Bearer token");
            return;
        }

        if (isBlank(subjectHeader)) {
            fail(ctx, 400, "X-Subject-Id header is required");
            return;
        }

        // Validate Bearer token and cross-check with headers
        if (!validateBearerToken(ctx, authHeader, subjectHeader, tenantHeader)) {
            return;
        }

        log.infov("🛡️ WebSocketValidationFilter PASSED: path={0}, ip={1}", path, clientIp);
        ctx.next();
    }

    /**
     * Validates JWT and ensures X-Subject-Id header matches the subject in the token
     */
    private boolean validateBearerToken(RoutingContext ctx, String authHeader,
                                        String subjectHeader, String tenantHeader) {
        try {
            String token = authHeader.substring(7).trim();
            JsonWebToken jwt = jwtParser.parse(token);

            String jwtSubject = jwt.getSubject();

            if (isBlank(jwtSubject)) {
                fail(ctx, 401, "JWT token missing subject");
                return false;
            }

            // Enforce X-Subject-Id matches JWT subject
            if (!subjectHeader.equals(jwtSubject)) {
                log.warnf("🔒 Subject mismatch - Header: {0}, JWT: {1}", subjectHeader, jwtSubject);
                fail(ctx, 401, "X-Subject-Id does not match JWT subject");
                return false;
            }

            boolean isAdmin = jwt.getGroups() != null && jwt.getGroups().contains(Role.ADMIN.name());

            // Propagate subject
//            ctx.request().headers().add("X-Subject-Id", subjectHeader);

            // === X-Tenant-Id check for non-admins ===
//            if (!isAdmin && isBlank(tenantHeader) && !jwt.containsClaim(Claim.TENANT_UUID.name())) {
            if (!isAdmin && isBlank(tenantHeader)) {
                fail(ctx, 400, "X-Tenant-Id / tenantUUID claim is required for non-admin users");
                return false;
            }

            // If tenant header was provided, it should match JWT (optional but good security)
            if (!isBlank(tenantHeader) && jwt.containsClaim(Claim.TENANT_UUID.name())) {
                String jwtTenant = jwt.getClaim(Claim.TENANT_UUID.name()).toString();
                if (!tenantHeader.equals(jwtTenant)) {
                    log.warnf("🔒 Tenant mismatch - Header: {0}, JWT: {1}", tenantHeader, jwtTenant);
                    fail(ctx, 401, "X-Tenant-Id does not match JWT tenant");
                    return false;
                }
            }

//            if (jwt.containsClaim(Claim.TENANT_UUID.name())) {
//                ctx.request().headers().add("X-Tenant-Id", jwt.getClaim(Claim.TENANT_UUID.name()).toString());
//            }

            log.debugf("✅ Bearer JWT validated - subject: {0}, admin: {1}", jwtSubject, isAdmin);
            return true;

        } catch (ParseException e) {
            log.warn("❌ Invalid JWT", e);
            fail(ctx, 401, "Invalid JWT token");
            return false;
        } catch (Exception e) {
            log.error("Bearer token error", e);
            fail(ctx, 401, "Authentication failed");
            return false;
        }
    }

    private boolean isRateLimited(String ip) {
        if (!rateLimitEnabled) return false;
        AtomicInteger counter = ipCache.get(ip, k -> new AtomicInteger(0));
        return counter != null && counter.incrementAndGet() > rateLimit;
    }

    private String getClientIp(RoutingContext ctx) {
        String xff = ctx.request().getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return ctx.request().remoteAddress().host();
    }

    private void fail(RoutingContext ctx, int status, String message) {
        ctx.response()
                .setStatusCode(status)
                .putHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN)
                .end(message);

    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}