package com.safecarealert.identity;

import java.util.Set;

public final class MonitorIdentity implements IdentityRecord {

    private final String subjectUUID;
    private final String tenantUUID;
    private final Set<String> roles;
    private final Set<String> allowedWorkplaces;

    public MonitorIdentity(
            String subjectUUID,
            String tenantUUID,
            Set<String> roles,
            Set<String> allowedWorkplaces
    ) {
        this.subjectUUID = subjectUUID;
        this.tenantUUID = tenantUUID;
        this.roles = roles;
        this.allowedWorkplaces = allowedWorkplaces;

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
    public LicenceStatus licenceStatus() {
        return LicenceStatus.NONE;
    }

    public Set<String> allowedWorkplaces() {
        return allowedWorkplaces;
    }



    /**
     * Safely extracts the workplace list from the JWT claim,
     * handling both single strings and collections of strings.
     */
    /*
    private Set<String> extractWorkplaces(JsonWebToken jwt) {
        if (!jwt.containsClaim(Claim.ALLOWED_WORKPLACES.name())) {
            return null;
        }

        Object raw = jwt.getClaim(Claim.ALLOWED_WORKPLACES.name());

        if (raw instanceof Iterable<?> iterable) {
            return StreamSupport.stream(iterable.spliterator(), false)
                    .map(Object::toString)
                    .collect(Collectors.toUnmodifiableSet());
        }

        if (raw instanceof String s && !s.isBlank()) {
            return Set.of(s);
        }

        return Collections.emptySet();
    }*/
}
