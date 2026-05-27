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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.time.LocalDateTime;

@Entity
@Table(name = "alert_actions")
public class AlertActions extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    public Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    public Alert alert;

    @Column(name = "type", nullable = false)
    public String type; // TRIGGERED, ACKNOWLEDGED, FALSE_ALARM, RESOLVED

    @Column(name = "payload", nullable = false)
    public String payload;

    @Column(name = "notes")
    public String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    public String createdBy;
}
