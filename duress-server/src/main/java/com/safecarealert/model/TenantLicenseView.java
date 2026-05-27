package com.safecarealert.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "v_tenant_license")
public class TenantLicenseView extends PanacheEntityBase {

    @Id
    @Column(name = "tenant_id")
    public Long tenantId;

    @Column(name = "subscription_id")
    public Long subscriptionId;

    @Column(name = "plan_name")
    public String planName;

    @Column(name = "max_devices")
    public Integer maxDevices;

    @Column(name = "allow_overage")
    public Boolean allowOverage;

    @Column(name = "overage_grace_days")
    public Integer overageGraceDays;

    @Column(name = "current_device_count")
    public Integer currentDeviceCount;

    @Column(name = "license_status")
    public String licenseStatus; // EXPIRED, VALID, OVER_LIMIT_GRACE, OVER_LIMIT_BLOCKED
}
