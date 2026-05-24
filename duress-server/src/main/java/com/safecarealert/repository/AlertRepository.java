package com.safecarealert.repository;

import com.safecarealert.model.Alert;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class AlertRepository implements PanacheRepository<Alert> {

    /**
     * Find all active (ongoing) alerts in a group
     */
    public List<Alert> findActiveByGroup(Long groupId) {
        return find("group.id = ?1 AND status = 'STARTED'", groupId).list();
    }

    /**
     * Find active alerts for a tenant
     */
    public List<Alert> findActiveByTenant(Long tenantId) {
        return find("tenant.id = ?1 AND status = 'STARTED'", tenantId).list();
    }

    /**
     * Find alert by correlation ID
     */
    public Optional<Alert> findByCorrelationId(String correlationId) {
        return find("correlationId = ?1", correlationId).firstResultOptional();
    }

    /**
     * Find recent alerts for a device
     */
    public List<Alert> findRecentByDevice(Long deviceId, int limit) {
        return find("device.id = ?1 ORDER BY startedAt DESC", deviceId)
                .page(0, limit)
                .list();
    }
}