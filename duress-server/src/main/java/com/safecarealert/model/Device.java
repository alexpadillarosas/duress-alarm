package com.safecarealert.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.time.LocalDateTime;

@Entity
@Table(name = "device")
public class Device extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "uuid", nullable = false, unique = true)
    public String uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    public Groups group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    public Tenant tenant;

    @Column(name = "name", nullable = false)
    public String name;

    @Column(name = "location", nullable = false)
    public String location;

    @Column(name = "serial_number", nullable = false, unique = true)
    public String serialNumber;

    @Column(name = "type", nullable = false)
    public String type = "COMPUTER";

    @Column(name = "status", nullable = false)
    public String status = "ACTIVE";

    @Column(name = "license_status", nullable = false)
    public String licenseStatus = "VALID";

    @CreationTimestamp
    @Column(name = "license_status_updated_at")
    public LocalDateTime licenseStatusUpdatedAt;

    @Column(name = "license_notes")
    public String licenseNotes;

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
