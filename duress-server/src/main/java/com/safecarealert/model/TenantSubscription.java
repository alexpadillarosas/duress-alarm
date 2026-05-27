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
@Table(name = "tenant_subscription")
public class TenantSubscription extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    public Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    public SubscriptionPlan plan;

    @Column(name = "status", nullable = false)
    public String status = "ACTIVE"; // ACTIVE, TRIAL, PAST_DUE, CANCELLED, EXPIRED, SUSPENDED

    @Column(name = "starts_at", nullable = false)
    public LocalDateTime startsAt;

    @Column(name = "ends_at")
    public LocalDateTime endsAt;

    @Column(name = "auto_renew", nullable = false)
    public Boolean autoRenew = true;

    @Column(name = "cancelled_at")
    public LocalDateTime cancelledAt;

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
