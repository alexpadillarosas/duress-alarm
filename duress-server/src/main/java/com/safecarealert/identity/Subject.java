package com.safecarealert.identity;

import java.util.Collections;
import java.util.Set;

public final class Subject {

    private final String subjectId;
    private final String tenantUUID;
    private final Set<Role> roles;

    private final String workplaceUUID;
    private final Set<String> groupUUIDs;
    private Set<String> allowedWorkplaces;

    public Subject(String subjectId,
                   String tenantUUID,
                   Set<Role> roles,
                   String workplaceUUID,
                   Set<String> groupUUIDs,
                   Set<String> allowedWorkplaces) {

        this.subjectId = subjectId;
        this.tenantUUID = tenantUUID;
        this.roles = roles != null ? Set.copyOf(roles) : Collections.emptySet();


        Set<String> safeGroups = groupUUIDs != null ? Set.copyOf(groupUUIDs) : Collections.emptySet();
        Set<String> safeAllowedWorkplaces = allowedWorkplaces != null ? Set.copyOf(allowedWorkplaces) : Collections.emptySet();


        if (this.roles.contains(Role.MONITOR)) {


            if (workplaceUUID != null ||  !safeGroups.isEmpty()) {
                System.out.println(
                        "NORMALIZATION: Monitor subject received device-style fields. " +
                                "Ignoring workplaceUUID=" + workplaceUUID +
                                ", groupUUIDs=" + safeGroups
                );
            }


            // MONITOR: ignore workplace + groups, use allowedWorkplaces ONLY
            this.workplaceUUID = null;
            this.groupUUIDs = Collections.emptySet();
            this.allowedWorkplaces = safeAllowedWorkplaces;

        } else if (this.roles.contains(Role.DEVICE)) {
            // DEVICE: must have workplace + groups, ignore allowedWorkplaces
            this.workplaceUUID = workplaceUUID;
            this.groupUUIDs = safeGroups;
            this.allowedWorkplaces = Collections.emptySet();

        } else {
            // DEFAULT (ADMIN / SUPPORT)
            this.workplaceUUID = workplaceUUID;
            this.groupUUIDs = safeGroups;
            this.allowedWorkplaces = safeAllowedWorkplaces;
        }


//        this.workplaceUUID = workplaceUUID;
//        this.groupUUIDs = groupUUIDs != null ? Set.copyOf(groupUUIDs) : Collections.emptySet();
//        this.allowedWorkplaces = allowedWorkplaces != null ? Set.copyOf(allowedWorkplaces) : Collections.emptySet();
    }

    public String getSubjectId() {
        return subjectId;
    }

    public String getTenantUUID() {
        return tenantUUID;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public String getWorkplaceUUID() {
        return workplaceUUID;
    }

    public Set<String> getGroupUUIDs() {
        return groupUUIDs;
    }

    public Set<String> getAllowedWorkplaces() {
        return allowedWorkplaces;
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

}
