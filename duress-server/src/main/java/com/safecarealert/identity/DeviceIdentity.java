package com.safecarealert.identity;

import java.util.Set;

public final class DeviceIdentity implements IdentityRecord {

    private final String subjectUUID;
    private final String tenantUUID;
    private final String workplaceUUID;
    private final String groupUUID;
    private final Set<String> roles;
    private final LicenceStatus licenceStatus;

    public DeviceIdentity(
            String subjectUUID,
            String tenantUUID,
            String workplaceUUID,
            String groupUUID,
            Set<String> roles,
            LicenceStatus licenceStatus
    ) {
        this.subjectUUID = subjectUUID;
        this.tenantUUID = tenantUUID;
        this.workplaceUUID = workplaceUUID;
        this.groupUUID = groupUUID;
        this.roles = roles;
        this.licenceStatus = licenceStatus;
    }

    @Override
    public String subjectUUID() {
        return subjectUUID;
    }

    @Override
    public String tenantUUID() {
        return tenantUUID;
    }

    public String workplaceUUID() {
        return workplaceUUID;
    }

    public String groupUUID() {
        return groupUUID;
    }

    @Override
    public Set<String> roles() {
        return roles;
    }

    @Override
    public LicenceStatus licenceStatus() {
        return licenceStatus;
    }
}
