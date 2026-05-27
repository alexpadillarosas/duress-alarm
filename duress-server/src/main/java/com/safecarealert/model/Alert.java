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
@Table(name = "alert")
public class Alert extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "uuid", nullable = false, unique = true)
    public String uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    public Device device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    public Tenant tenant;

    @Column(name = "message", nullable = false)
    public String message;

    @Column(name = "status", nullable = false)
    public String status = "TRIGGERED"; // TRIGGERED, ACKNOWLEDGED, RESOLVED, FALSE_ALARM, CANCELLED

    @Column(name = "severity", nullable = false)
    public String severity = "CRITICAL";

    @Column(name = "is_test", nullable = false)
    public Boolean isTest = false;

    @Column(name = "test_reason")
    public String testReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "created_by")
    public String createdBy;

    @UpdateTimestamp
    @Column(name = "updated_at")
    public LocalDateTime updatedAt;
}
