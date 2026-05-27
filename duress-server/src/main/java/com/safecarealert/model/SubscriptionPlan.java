package com.safecarealert.model;


import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_plan")
public class SubscriptionPlan extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "name", nullable = false, unique = true)
    public String name; // Starter, Professional, Enterprise

    @Column(name = "description")
    public String description;

    @Column(name = "max_devices", nullable = false)
    public Integer maxDevices;

    @Column(name = "max_users", nullable = false)
    public Integer maxUsers = 999999;

    @Column(name = "max_workplaces")
    public Integer maxWorkplaces = 0;

    @Column(name = "billing_cycle", nullable = false)
    public String billingCycle = "MONTHLY"; // MONTHLY, YEARLY

    @Column(name = "price_cents", nullable = false)
    public Integer priceCents;

    @Column(name = "currency", nullable = false)
    public String currency = "AUD";

    @Column(name = "allow_overage", nullable = false)
    public Boolean allowOverage = false;

    @Column(name = "overage_grace_days", nullable = false)
    public Integer overageGraceDays = 14;

    @Column(name = "features")
    public String features; // JSON configuration mapping string

    @Column(name = "status", nullable = false)
    public String status = "ACTIVE"; // ACTIVE, INACTIVE, DEPRECATED

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    public LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    public LocalDateTime updatedAt;
}
