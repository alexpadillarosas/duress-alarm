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
import java.time.LocalDateTime;

@Entity
@Table(name = "tenant_subscription_history")
public class TenantSubscriptionHistory extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    public Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_subscription_id", nullable = false)
    public TenantSubscription tenantSubscription;

    @Column(name = "old_plan_id")
    public Long oldPlanId;

    @Column(name = "new_plan_id")
    public Long newPlanId;

    @Column(name = "old_status")
    public String oldStatus;

    @Column(name = "new_status")
    public String newStatus;

    @Column(name = "change_type", nullable = false)
    public String changeType; // UPGRADE, DOWNGRADE, RENEWAL, CANCEL, SUSPEND

    @Column(name = "reason")
    public String reason;

    @Column(name = "changed_by", nullable = false)
    public String changedBy;

    @CreationTimestamp
    @Column(name = "changed_at", updatable = false)
    public LocalDateTime changedAt;
}
