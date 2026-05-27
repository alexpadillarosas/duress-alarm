package com.safecarealert.model;


import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenant")
public class Tenant extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "uuid", nullable = false, unique = true)
    public String uuid;

    @Column(name = "name", nullable = false, unique = true)
    public String name;

    @Column(name = "description")
    public String description;

    @Column(name = "status", nullable = false)
    public String status; // ACTIVE, INACTIVE, SUSPENDED, TRIAL, EXPIRED, PENDING

    @Column(name = "current_device_count", nullable = false)
    public Integer currentDeviceCount = 0;

    @Column(name = "license_status")
    public String licenseStatus = "VALID";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    public String createdBy;

    @UpdateTimestamp
    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @Column(name = "updated_by", nullable = false)
    public String updatedBy;
}
