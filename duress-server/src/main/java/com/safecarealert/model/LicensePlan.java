package com.safecarealert.model;

import jakarta.persistence.*;

@Entity
@Table(name = "license_plans")
public class LicensePlan extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public String name;

    @Column
    public String description;

    @Column(name = "default_max_devices")
    public Integer defaultMaxDevices;

    @Column(name = "default_max_monitors")
    public Integer defaultMaxMonitors;

    @Column(name = "default_max_workplaces")
    public Integer defaultMaxWorkplaces;

    @Column
    @Enumerated(EnumType.STRING)
    public Status status;
}