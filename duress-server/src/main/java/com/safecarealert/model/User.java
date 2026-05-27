package com.safecarealert.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "user")
public class User extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "uuid", nullable = false, unique = true)
    public String uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    public Tenant tenant;

    @Column(name = "username", nullable = false, unique = true)
    public String username;

    @Column(name = "email", nullable = false, unique = true)
    public String email;

    @Column(name = "first_name")
    public String firstName;

    @Column(name = "last_name")
    public String lastName;

    @Column(name = "phone")
    public String phone;

    @Column(name = "status", nullable = false)
    public String status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    public String createdBy;

    @UpdateTimestamp
    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @Column(name = "updated_by")
    public String updatedBy;

    // Relationship with roles
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "role")
    public Set<String> roles = new HashSet<>();

    // Helper methods
    public boolean hasRole(String roleName) {
        return roles.stream().anyMatch(r -> r.equalsIgnoreCase(roleName));
    }

    public boolean isMonitor() {
        return hasRole("MONITOR");
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    public boolean isSupport() {
        return hasRole("SUPPORT");
    }
}