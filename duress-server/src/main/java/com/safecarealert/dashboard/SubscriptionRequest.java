package com.safecarealert.dashboard;

import com.safecarealert.alerts.GroupKey;
import java.util.List;

/**
 * Action-based request from a Dashboard to subscribe to specific levels.
 * tenantId: "HOSPITAL_A", workplaceId: "*", groupId: "*" -> Watch whole Hospital
 */
public record SubscriptionRequest(
        SubscriptionAction action,          // e.g., "SUBSCRIBE" or "UNSUBSCRIBE"
        List<MonitorScope> monitorScopes
) {
    public record MonitorScope(
            String tenantId,
            String workplaceId, // Specific ID or "*" for all
            String groupId      // Specific ID or "*" for all
    ) {
        /**
         * Checks if a specific alert (GroupKey) matches any of our subscribed scopes.
         */
        public boolean matches(GroupKey key) {
            boolean tMatch = tenantId.equals(key.tenantId());
            boolean wMatch = "*".equals(workplaceId) || workplaceId.equals(key.workplaceId());
            boolean gMatch = "*".equals(groupId) || groupId.equals(key.groupId());
            return tMatch && wMatch && gMatch;
        }
    }

    /**
     * Checks if this subscription matches the given group key
     */
    public boolean matches(GroupKey key) {
        if (monitorScopes == null || key == null) return false;
        return monitorScopes.stream().anyMatch(scope -> scope.matches(key));
    }

}

