package com.safecarealert.websocket;

import com.safecarealert.dashboard.SubscriptionRequest;
import com.safecarealert.identity.Claim;
import com.safecarealert.identity.LicenceStatus;
import com.safecarealert.identity.Role;
import com.safecarealert.identity.Subject;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import java.util.Set;

/**
 * WebSocketIdentityValidator
 * --------------------------
 * Centralized identity validation for WebSocket endpoints.
 * <p>
 * Responsibilities:
 *   • DEVICE validation for AlertSocket (tenant/workplace/group/subject match)
 *   • MONITOR validation for MonitorSocket (subscription scope authorization)
 * <p>
 * This class performs *identity-level* validation only.
 * Protocol-level validation is handled by MessageRouter/handlers.
 * <p>
 * All logs preserved and improved.
 */
@ApplicationScoped
public class WebSocketIdentityValidator {

    @Inject
    Logger log;

    // -------------------------------------------------------------------------
    // DEVICE VALIDATION (AlertSocket)
    // -------------------------------------------------------------------------

    /**
     * Validates that the WebSocket connection belongs to a DEVICE and that
     * the CONNECT query parameters match the identity attributes.
     * <p>
     * This is strict: any mismatch results in rejection.
     */
    public boolean validateDeviceContext(SecurityIdentity identity,
                                         WebSocketConnection connection,
                                         String tenantId,
                                         String workplaceId,
                                         String groupId,
                                         String subjectId) {

        Subject subject = identity.getAttribute(Claim.SUBJECT.name());
        if (subject == null) {
            log.error("❌ DEVICE_REJECTED: No Subject attached to SecurityIdentity");
            return false;
        }

        // Must be a DEVICE
        if (!subject.hasRole(Role.DEVICE)) {
            log.error("❌ DEVICE_REJECTED: Subject is not a DEVICE");
            return false;
        }

        // License must be ACTIVE
        LicenceStatus licenceStatus = identity.getAttribute(Claim.LICENCE_STATUS.name());
        if (licenceStatus != LicenceStatus.ACTIVE) {
            log.errorf("❌ DEVICE_REJECTED: License invalid for subject %s", subject.getSubjectId());
            return false;
        }

        // Required CONNECT fields
        if (isBlank(tenantId) || isBlank(workplaceId) || isBlank(groupId) || isBlank(subjectId)) {
            log.errorf("❌ DEVICE_REJECTED: Missing required CONNECT fields. tenant=%s, workplace=%s, group=%s, subject=%s",
                    tenantId, workplaceId, groupId, subjectId);
            return false;
        }

        // Expected values from identity
        String expectedTenant    = subject.getTenantUUID();
        String expectedWorkplace = subject.getWorkplaceUUID();
        String expectedSubject   = subject.getSubjectId();
        Set<String> groups       = subject.getGroupUUIDs();

        boolean valid = true;

        if (!equals(expectedTenant, tenantId)) {
            logMismatch("tenant", expectedTenant, tenantId, connection.id());
            valid = false;
        }

        if (!equals(expectedWorkplace, workplaceId)) {
            logMismatch("workplace", expectedWorkplace, workplaceId, connection.id());
            valid = false;
        }

//        if (groups == null || !groups.contains(groupId)) {
//        groups is never null, subject.getGroupUUIDs() will return an empty set if null
        if (!groups.contains(groupId)) {
            logMismatch("group", groups.toString(), groupId, connection.id());
            valid = false;
        }

        if (!equals(expectedSubject, subjectId)) {
            logMismatch("subject", expectedSubject, subjectId, connection.id());
            valid = false;
        }

        return valid;
    }

    // -------------------------------------------------------------------------
    // MONITOR VALIDATION (MonitorSocket)
    // -------------------------------------------------------------------------

    /**
     * Validates that a MONITOR/SUPPORT/ADMIN is allowed to subscribe to the
     * requested scopes.
     *
     * ADMIN → full access
     * SUPPORT → full access
     * MONITOR → restricted to allowedWorkplaces + same tenant + group='*'
     */
    public boolean validateMonitorContext(SecurityIdentity identity, SubscriptionRequest request) {

        Subject subject = identity.getAttribute(Claim.SUBJECT.name());
        if (subject == null) {
            log.error("❌ MONITOR_REJECTED: No Subject attached to SecurityIdentity");
            return false;
        }

        // ADMIN → full access
        if (subject.hasRole(Role.ADMIN)) {
            return true;
        }

        // SUPPORT → full access
        if (subject.hasRole(Role.SUPPORT)) {
            return true;
        }

        // MONITOR → must validate scopes
        if (subject.hasRole(Role.MONITOR)) {

            String subjectTenant = subject.getTenantUUID();
            if (subjectTenant == null || subjectTenant.isBlank()) {
                log.error("❌ MONITOR_REJECTED: Missing tenantId in identity");
                return false;
            }


            Set<String> allowedWorkplaces = subject.getAllowedWorkplaces();
            if (allowedWorkplaces == null || allowedWorkplaces.isEmpty()) {
                log.error("❌ MONITOR_REJECTED: No allowed workplaces in identity");
                return false;
            }
            // a full tenant monitor will be allowedWorkplaces == ["*"]
            boolean fullTenantMonitor = allowedWorkplaces.contains("*");

            if (request.monitorScopes() == null || request.monitorScopes().isEmpty()) {
                log.error("❌ MONITOR_REJECTED: No monitor scopes provided");
                return false;
            }

            /*
             * A monitor can send multiple scopes such:
             *      [
             *          { "tenantId": "T1", "workplaceId": "W1", "groupId": "*" },
             *          { "tenantId": "T1", "workplaceId": "W2", "groupId": "*" }
             *      ]
             * The for loop will check each scope one by one, if one fails the whole request fails.
             *
             */
            for (SubscriptionRequest.MonitorScope monitorScope : request.monitorScopes()) {
                //Are you trying to monitor the tenant you belong to?
                if (!equals(subjectTenant, monitorScope.tenantId())) {
                    log.errorf("❌ MONITOR_REJECTED: Tenant mismatch. Expected %s but got %s",
                            subjectTenant,
                            monitorScope.tenantId());
                    return false;
                }

                String requestedWorkplace = monitorScope.workplaceId();

                if (!fullTenantMonitor){
                    boolean requestingAllWorkplaces = "*".equals(requestedWorkplace);

                    if (requestingAllWorkplaces) {
                        log.errorf(
                                "❌ MONITOR_REJECTED: Monitor %s cannot subscribe to '*' workplaces without full access",
                                subject.getSubjectId()
                        );
                        return false;
                    }
                    //Are you requesting a workplace you are allowed to see?
                    if (!allowedWorkplaces.contains(requestedWorkplace)) {
                        log.errorf("❌ MONITOR_REJECTED: Workplace %s not allowed for monitor %s",
                                requestedWorkplace,
                                subject.getSubjectId()
                        );
                        return false;
                    }

                }

                // Monitor Must subscribe to all groups
                if (!"*".equals(monitorScope.groupId())) {
                    log.error("❌ MONITOR_REJECTED: Monitors must subscribe with group='*'");
                    return false;
                }
            }

            return true;
        }

        log.error("❌ MONITOR_REJECTED: Subject roles not allowed for monitor socket");
        return false;
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private void logMismatch(String field, String expected, String actual, String connId) {
        log.errorf("🔒 SECURITY_MISMATCH: %s mismatch for connection %s. Identity has [%s] but CONNECT provided [%s]",
                field, connId, expected, actual);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean equals(String a, String b) {
        return a != null && a.equals(b);
    }
}
