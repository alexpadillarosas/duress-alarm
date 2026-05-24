package com.safecarealert.model;

import jakarta.persistence.*;

@Entity
@Table(name = "workplaces")
public class Workplace extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    public Tenant tenant;

    @Column(nullable = false)
    public String name;

    @Column
    public String description;

    @Column
    @Enumerated(EnumType.STRING)
    public Status status;
}