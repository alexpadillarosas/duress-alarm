package com.safecarealert.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
public class Alert extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    public Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    public Device device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    public Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "started_by_user_id")
    public User startedByUser;

    @Column(name = "correlation_id", unique = true, length = 100)
    public String correlationId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    public AlertStatus status;

    public String location;
    public String person;

    @Column(length = 500)
    public String message;

    @Column(name = "started_at")
    public LocalDateTime startedAt;

    // === Acknowledgment Fields ===
    @Column(name = "acknowledged_at")
    public LocalDateTime acknowledgedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acknowledged_by_id")
    public User acknowledgedBy;

    // === Resolution Fields ===
    @Column(name = "resolved_at")
    public LocalDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_id")
    public User resolvedBy;

    @Column(name = "resolution_note", length = 500)
    public String resolutionNote;
}