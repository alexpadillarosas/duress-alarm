package com.safecarealert.repository;

import com.safecarealert.model.Tenant;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class TenantRepository implements PanacheRepository<Tenant> {

    /**
     * Find tenant by name (case-insensitive)
     */
    public Optional<Tenant> findByName(String name) {
        return find("LOWER(name) = LOWER(?1)", name).firstResultOptional();
    }

    /**
     * Find tenant by UUID string
     */
    public Optional<Tenant> findByUuid(String uuid) {
        return find("uuid = ?1", uuid).firstResultOptional();
    }
}