package com.safecarealert.identity;

import java.util.Set;

public final class SupportIdentity implements IdentityRecord {

    private final String subjectUUID;
    private final String tenantUUID;
    private final Set<String> roles;

    public SupportIdentity(String subjectUUID, String tenantUUID, Set<String> roles) {
        this.subjectUUID = subjectUUID;
        this.tenantUUID = tenantUUID;
        this.roles = roles;
    }

    @Override
    public String subjectUUID() { return subjectUUID; }
    @Override
    public String tenantUUID() { return tenantUUID; }

    @Override
    public String workplaceUUID() {
        return "";
    }

    @Override
    public String groupUUID() {
        return "";
    }

    @Override
    public Set<String> roles() { return roles; }
    @Override
    public LicenceStatus licenceStatus() { return LicenceStatus.NONE; }
}
