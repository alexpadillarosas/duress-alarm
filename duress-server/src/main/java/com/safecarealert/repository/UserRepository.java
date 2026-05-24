package com.safecarealert.repository;

import com.safecarealert.model.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class UserRepository implements PanacheRepository<User> {

    /**
     * Find user by email within a tenant (case-insensitive)
     */
    public Optional<User> findByEmailAndTenant(String email, Long tenantId) {
        return find("LOWER(email) = LOWER(?1) AND tenant.id = ?2 AND isActive = true",
                email, tenantId).firstResultOptional();
    }

    /**
     * Find all active users in a tenant
     */
    public List<User> findActiveByTenant(Long tenantId) {
        return find("tenant.id = ?1 AND isActive = true", tenantId).list();
    }

    /**
     * Find users by role name
     */
    public List<User> findByRole(String roleName) {
        return find("roles.name = ?1 AND isActive = true", roleName).list();
    }
}