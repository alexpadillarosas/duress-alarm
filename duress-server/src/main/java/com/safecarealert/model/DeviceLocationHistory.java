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
@Table(name = "device_location_history")
public class DeviceLocationHistory extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    public Device device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    public Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workplace_id", nullable = false)
    public Workplace workplace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    public Groups group;

    @CreationTimestamp
    @Column(name = "assigned_at", updatable = false, nullable = false)
    public LocalDateTime assignedAt;

    @Column(name = "unassigned_at")
    public LocalDateTime unassignedAt; // NULL means this is the currently active location

    @Column(name = "assigned_by", nullable = false)
    public String assignedBy;

    @Column(name = "notes")
    public String notes;
}
