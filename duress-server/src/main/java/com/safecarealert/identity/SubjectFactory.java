package com.safecarealert.identity;

import com.safecarealert.utils.StringUtils;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class SubjectFactory {

    public Subject fromIdentityRecord(IdentityRecord record) {
        if (record == null) {
            return null;
        }

        return switch (record) {
            case DeviceIdentity d -> buildDeviceSubject(d);
            case MonitorIdentity m -> buildMonitorSubject(m);
            case AdminIdentity a -> buildAdminSubject(a);
            case SupportIdentity s -> buildSupportSubject(s);
        };
    }

    private Subject buildDeviceSubject(DeviceIdentity d) {
        Set<Role> roles = toRoles(d.roles());


        return new Subject(
                StringUtils.normalize(d.subjectUUID()),
                StringUtils.normalize(d.tenantUUID()),
                roles,
                StringUtils.normalize(d.workplaceUUID()),
                Set.of(StringUtils.normalize(d.groupUUID())),
                Collections.emptySet()
        );

    }


    private Subject buildMonitorSubject(MonitorIdentity m) {
        Set<Role> roles = toRoles(m.roles());

        Set<String> cleanedWorkplaces = m.allowedWorkplaces() == null
                ? Collections.emptySet()
                : m.allowedWorkplaces()
                .stream()
                .map(StringUtils::normalize)
                .collect(Collectors.toSet());

        return new Subject(
                StringUtils.normalize(m.subjectUUID()),
                StringUtils.normalize(m.tenantUUID()),
                roles,
                null,
                Collections.emptySet(),
                cleanedWorkplaces
        );
    }

    private Subject buildAdminSubject(AdminIdentity a) {
        Set<Role> roles = toRoles(a.roles());

        return new Subject(
                StringUtils.normalize(a.subjectUUID()),
                null,
                roles,
                null,
                Collections.emptySet(),
                Collections.emptySet()
        );
    }

    private Subject buildSupportSubject(SupportIdentity s) {
        Set<Role> roles = toRoles(s.roles());

        return new Subject(
                StringUtils.normalize(s.subjectUUID()),
                StringUtils.normalize(s.tenantUUID()),
                roles,
                null,
                Collections.emptySet(),
                Collections.emptySet()
        );
    }

    private Set<Role> toRoles(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return EnumSet.noneOf(Role.class);
        }
        return roleNames.stream()
                .map(String::valueOf)
                .map(StringUtils::normalize)
                .map(Role::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Role.class)));
    }


//    private String normalize(String value) {
//        if (value == null) return null;
//
//        value = value.trim();
//
//        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
//            value = value.substring(1, value.length() - 1);
//        }
//
//        return value;
//    }
}
