package com.safecarealert.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "tenant_licenses")
public class TenantLicense extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    public Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "license_plan_id", nullable = false)
    public LicensePlan licensePlan;

    @Column(name = "max_devices")
    public Integer maxDevices;

    @Column(name = "max_monitors")
    public Integer maxMonitors;

    @Column(name = "max_workplaces")
    public Integer maxWorkplaces;

    @Column(name = "valid_from")
    public LocalDate validFrom;

    @Column(name = "valid_to")
    public LocalDate validTo;

    @Column
    @Enumerated(EnumType.STRING)
    public Status status;
}