package com.safecarealert.model;

import jakarta.persistence.*;

@Entity
@Table(name = "\"groups\"")   // quotes needed because "group" is a reserved word
public class Group extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workplace_id", nullable = false)
    public Workplace workplace;

    @Column(nullable = false)
    public String name;

    @Column
    public String description;

    @Column
    @Enumerated(EnumType.STRING)
    public Status status;
}