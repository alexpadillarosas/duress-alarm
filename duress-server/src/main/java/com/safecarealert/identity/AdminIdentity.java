package com.safecarealert.identity;

import java.util.Set;

public final class AdminIdentity implements IdentityRecord {

    private final String subjectUUID;
    private final Set<String> roles;

    public AdminIdentity(String subjectUUID, Set<String> roles) {
        this.subjectUUID = subjectUUID;
        this.roles = roles;
    }

    @Override
    public String subjectUUID() { return subjectUUID; }

    @Override
    public String tenantUUID() { return null; }

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
