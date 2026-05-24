package com.safecarealert.model;

import jakarta.persistence.*;

@Entity
@Table(name = "devices")
public class Device extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    public Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workplace_id")
    public Workplace workplace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    public Group group;

    @Column(nullable = false)
    public String name;

    @Column(name = "serial_number", nullable = false, unique = true)
    public String serialNumber;

    @Column
    @Enumerated(EnumType.STRING)
    public Status status;
}