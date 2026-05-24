package com.safecarealert.repository;

import com.safecarealert.model.Device;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class DeviceRepository implements PanacheRepository<Device> {

    /**
     * Find all active devices in a specific group
     */
    public List<Device> findActiveByGroup(Long groupId) {
        return find("group.id = ?1 AND isDeleted = false AND status = 'ACTIVE'",
                groupId).list();
    }

    /**
     * Find all active devices in a workplace
     */
    public List<Device> findActiveByWorkplace(Long workplaceId) {
        return find("workplace.id = ?1 AND isDeleted = false AND status = 'ACTIVE'",
                workplaceId).list();
    }

    /**
     * Find device by serial number
     */
    public Optional<Device> findBySerialNumber(String serialNumber) {
        return find("serialNumber = ?1 AND isDeleted = false", serialNumber)
                .firstResultOptional();
    }

    /**
     * Find all active devices for a tenant
     */
    public List<Device> findActiveByTenant(Long tenantId) {
        return find("tenant.id = ?1 AND isDeleted = false AND status = 'ACTIVE'",
                tenantId).list();
    }
}