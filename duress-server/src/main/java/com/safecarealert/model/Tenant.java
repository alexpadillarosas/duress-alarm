package com.safecarealert.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tenants")
public class Tenant extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public String name;

    @Column(nullable = false)
    public String description;

    @Column
    @Enumerated(EnumType.STRING)
    public Status status;
}