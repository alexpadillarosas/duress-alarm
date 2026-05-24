package com.safecarealert.model;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    public Tenant tenant;

    @Column(nullable = false, unique = true)
    public String email;

    @Column(name = "display_name")
    public String displayName;

    @Column
    @Enumerated(EnumType.STRING)
    public Status status;

    @ManyToMany
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    public Set<Role> roles = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "monitor_workplaces",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "workplace_id")
    )
    public Set<Workplace> monitoredWorkplaces = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "monitor_groups",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    public Set<Group> monitoredGroups = new HashSet<>();
}