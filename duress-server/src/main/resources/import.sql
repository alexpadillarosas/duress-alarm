-- =============================================
-- 1. TENANTS
-- =============================================
INSERT INTO tenants (name, description, status, created_by, created_at, updated_at)
VALUES
    ('Acme Security', 'Headquarters - Sydney', 'ACTIVE', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Beta Logistics', 'Distribution & Warehousing', 'ACTIVE', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('City Hospital', 'Healthcare Security', 'ACTIVE', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =============================================
-- 2. LICENSE PLANS
-- =============================================
INSERT INTO license_plans (name, description, default_max_devices, default_max_monitors,
                           default_max_workplaces, status, created_by, created_at, updated_at)
VALUES
    ('Professional', 'Standard duress alarm solution', 100, 50, 20, 'ACTIVE', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Enterprise', 'Advanced multi-site with analytics', 500, 200, 100, 'ACTIVE', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =============================================
-- 3. TENANT LICENSES
-- =============================================
INSERT INTO tenant_licenses (tenant_id, license_plan_id, max_devices, max_monitors,
                             max_workplaces, valid_from, valid_to, status,
                             created_by, created_at, updated_at)
VALUES
    (1, 1, 85, 45, 18, DATE('now', '-2 months'), DATE('now', '+10 months'), 'ACTIVE', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 2, 320, 120, 45, DATE('now', '-1 month'), DATE('now', '+14 months'), 'ACTIVE', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 1, 60, 30, 12, DATE('now', '-3 months'), DATE('now', '+9 months'), 'ACTIVE', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =============================================
-- 4. WORKPLACES
-- =============================================
INSERT INTO workplaces (tenant_id, name, description, status, created_by, created_at, updated_at)
VALUES
    (1, 'Main Warehouse', 'Primary distribution center', 'ACTIVE', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'Head Office', 'Corporate headquarters', 'ACTIVE', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'North Depot', 'Secondary storage facility', 'ACTIVE', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'Sydney Hub', 'Major logistics terminal', 'ACTIVE', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'Melbourne Depot', 'Southern operations', 'ACTIVE', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'Main Hospital Campus', 'Primary medical facility', 'ACTIVE', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'Emergency Wing', '24/7 Emergency Department', 'ACTIVE', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =============================================
-- 5. GROUPS
-- =============================================
INSERT INTO "groups" (workplace_id, name, description, status, created_by, created_at, updated_at)
VALUES
    (1, 'Night Shift A', 'Night security team', 'ACTIVE', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'Day Shift A', 'Day security team', 'ACTIVE', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'Reception Team', 'Front desk and lobby staff', 'ACTIVE', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'Admin Security', 'Office security personnel', 'ACTIVE', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, 'Loading Crew', 'Dock and loading bay team', 'ACTIVE', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, 'Warehouse Patrol', 'Internal patrol team', 'ACTIVE', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (6, 'ER Security', 'Emergency department security', 'ACTIVE', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (7, 'Ward Patrol', 'General ward security team', 'ACTIVE', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =============================================
-- 6. ROLES
-- =============================================
INSERT INTO roles (name, created_by, created_at, updated_at)
VALUES
    ('ADMIN', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('SUPERVISOR', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('OPERATOR', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('VIEWER', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =============================================
-- 7. USERS
-- =============================================
INSERT INTO users (tenant_id, email, display_name, status, created_by, created_at, updated_at)
VALUES
    (1, 'john.smith@acme.com', 'John Smith', 'ACTIVE', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'sarah.patel@acme.com', 'Sarah Patel', 'ACTIVE', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 'mike.ross@acme.com', 'Michael Ross', 'ACTIVE', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'david.kim@beta.com', 'David Kim', 'ACTIVE', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'lisa.wong@beta.com', 'Lisa Wong', 'ACTIVE', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'emma.brown@hospital.com', 'Dr. Emma Brown', 'ACTIVE', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =============================================
-- 8. DEVICES
-- =============================================
INSERT INTO devices (tenant_id, workplace_id, group_id, name, serial_number, status,
                     created_by, created_at, updated_at)
VALUES
    (1, 1, 1, 'Warehouse Panic 01', 'DS-8741', 'ACTIVE', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 1, 1, 'Warehouse Panic 02', 'DS-8742', 'ACTIVE', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 2, 3, 'Reception Button', 'DS-1123', 'ACTIVE', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 4, 5, 'Loading Bay 03', 'DS-BL01', 'ACTIVE', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 6, 7, 'ER Duress Button', 'DS-H01', 'ACTIVE', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =============================================
-- 9. ALERTS (Fixed - using only AlertStatus values)
-- =============================================
INSERT INTO alerts (tenant_id, device_id, group_id, started_by_user_id, correlation_id,
                    status, location, person, message, started_at,
                    acknowledged_at, acknowledged_by_id, resolved_at, resolved_by_id,
                    resolution_note, created_by, created_at, updated_at)
VALUES
    (1, 1, 1, NULL, 'CORR-20250517-001', 'ACTIVE',
     'Warehouse Zone B - Aisle 12', 'Unknown',
     'Duress button pressed - possible intruder', CURRENT_TIMESTAMP,
     NULL, NULL, NULL, NULL, NULL, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    (1, 3, 3, 2, 'CORR-20250517-002', 'ACKNOWLEDGED',
     'Head Office - Reception Area', 'Sarah Patel',
     'Staff member under duress', datetime('now', '-25 minutes'),
     datetime('now', '-18 minutes'), 1, NULL, NULL, NULL, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    (2, 4, 5, NULL, 'CORR-20250517-003', 'RESOLVED',
     'Loading Bay 3', 'Unknown Driver',
     'Threat reported by driver', datetime('now', '-2 hours'),
     datetime('now', '-110 minutes'), 4, datetime('now', '-90 minutes'), 5,
     'False alarm - verbal dispute', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =============================================
-- 10. ALERT MESSAGES
-- =============================================
INSERT INTO alert_messages (alert_id, sender_user_id, sender_device_id, message_type,
                            note, sent_at, created_by, created_at, updated_at)
VALUES
    (1, NULL, 1, 'DURESS_TRIGGER', 'Duress button activated', CURRENT_TIMESTAMP, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    (2, 2, NULL, 'DURESS_TRIGGER', 'Help! Someone is threatening me', datetime('now', '-25 minutes'), 'SYSTEM', datetime('now', '-25 minutes'), datetime('now', '-25 minutes')),
    (2, 1, NULL, 'ACKNOWLEDGMENT', 'Acknowledged - John Smith responding', datetime('now', '-18 minutes'), 'SYSTEM', datetime('now', '-18 minutes'), datetime('now', '-18 minutes')),

    (3, NULL, 4, 'DURESS_TRIGGER', 'Driver reported aggressive individual', datetime('now', '-2 hours'), 'SYSTEM', datetime('now', '-2 hours'), datetime('now', '-2 hours')),
    (3, 5, NULL, 'RESOLUTION', 'Situation resolved - verbal dispute only', datetime('now', '-90 minutes'), 'SYSTEM', datetime('now', '-90 minutes'), datetime('now', '-90 minutes'));