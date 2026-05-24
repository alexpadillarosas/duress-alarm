package com.safecarealert.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.Instant;

@MappedSuperclass
public abstract class AuditableEntity extends PanacheEntityBase {

    @Column(name = "created_by", length = 100, updatable = false)
//    @ColumnOrder(100)
    public String createdBy;

    @Column(name = "created_at", updatable = false)
    public Instant createdAt;

    @Column(name = "updated_by", length = 100)
    public String updatedBy;

    @Column(name = "updated_at")
    public Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        this.createdAt = now;
        this.updatedAt = now;

        if (this.createdBy == null || this.createdBy.isBlank()) {
            this.createdBy = "SYSTEM";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();

        if (this.updatedBy == null || this.updatedBy.isBlank()) {
            this.updatedBy = "SYSTEM";
        }
    }
}