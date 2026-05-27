-- =============================================
-- SUBSCRIPTION PLANS
-- =============================================
INSERT INTO subscription_plan (name, description, max_devices, max_users,
                               billing_cycle, price_cents, currency, allow_overage,
                               overage_grace_days, status)
VALUES
    ('Starter', 'Small teams', 10, 20, 'MONTHLY', 9900, 'AUD', 1, 14, 'ACTIVE'),
    ('Professional', 'Growing businesses', 50, 100, 'MONTHLY', 29900, 'AUD', 1, 14, 'ACTIVE'),
    ('Enterprise', 'Large organizations', 200, 500, 'MONTHLY', 79900, 'AUD', 1, 30, 'ACTIVE');


-- =============================================
-- TENANTS + SUBSCRIPTIONS
-- =============================================
INSERT INTO tenant (uuid, name, description, status, created_by, updated_by)
VALUES
    ('tnt-001', 'Acme Corp', 'Main manufacturing company', 'ACTIVE', 'admin', 'admin'),
    ('tnt-002', 'Beta Solutions', 'IT consulting firm', 'ACTIVE', 'admin', 'admin');

INSERT INTO tenant_subscription (tenant_id, plan_id, status, starts_at, created_by, updated_by)
VALUES
    (1, 2, 'ACTIVE', CURRENT_TIMESTAMP, 'admin', 'admin'),   -- Professional
    (2, 1, 'ACTIVE', CURRENT_TIMESTAMP, 'admin', 'admin');   -- Starter


-- =============================================
-- WORKPLACES
-- =============================================
INSERT INTO workplace (uuid, tenant_id, name, address, status, created_by, updated_by) VALUES
                                                                                           ('wp-001', 1, 'Sydney Headquarters', '123 George St, Sydney NSW 2000', 'ACTIVE', 'admin', 'admin'),
                                                                                           ('wp-002', 1, 'Melbourne Factory', '45 Industrial Rd, Melbourne VIC 3000', 'ACTIVE', 'admin', 'admin'),
                                                                                           ('wp-003', 1, 'Brisbane Warehouse', '78 Logistics Ave, Brisbane QLD 4000', 'ACTIVE', 'admin', 'admin'),
                                                                                           ('wp-b1',  2, 'Perth Office', '200 St Georges Terrace, Perth WA 6000', 'ACTIVE', 'admin', 'admin'),
                                                                                           ('wp-b2',  2, 'Adelaide Branch', '55 King William St, Adelaide SA 5000', 'ACTIVE', 'admin', 'admin');


-- =============================================
-- GROUPS
-- =============================================
INSERT INTO groups (uuid, tenant_id, workplace_id, name, status, created_by, updated_by) VALUES
                                                                                             ('grp-s1', 1, 1, 'Engineering Floor', 'ACTIVE', 'admin', 'admin'),
                                                                                             ('grp-s2', 1, 1, 'Management Floor', 'ACTIVE', 'admin', 'admin'),
                                                                                             ('grp-s3', 1, 1, 'Production Line A', 'ACTIVE', 'admin', 'admin'),
                                                                                             ('grp-m1', 1, 2, 'Assembly Line 1', 'ACTIVE', 'admin', 'admin'),
                                                                                             ('grp-m2', 1, 2, 'Quality Control', 'ACTIVE', 'admin', 'admin'),
                                                                                             ('grp-m3', 1, 2, 'Maintenance Team', 'ACTIVE', 'admin', 'admin'),
                                                                                             ('grp-b1', 1, 3, 'Storage Operations', 'ACTIVE', 'admin', 'admin'),
                                                                                             ('grp-b2', 1, 3, 'Dispatch Team', 'ACTIVE', 'admin', 'admin'),
                                                                                             ('grp-b3', 1, 3, 'Security & Safety', 'ACTIVE', 'admin', 'admin'),
                                                                                             ('grp-p1', 2, 4, 'Development Team', 'ACTIVE', 'admin', 'admin'),
                                                                                             ('grp-p2', 2, 4, 'Support Team', 'ACTIVE', 'admin', 'admin'),
                                                                                             ('grp-p3', 2, 4, 'Sales & Marketing', 'ACTIVE', 'admin', 'admin'),
                                                                                             ('grp-a1', 2, 5, 'Consulting Floor', 'ACTIVE', 'admin', 'admin'),
                                                                                             ('grp-a2', 2, 5, 'Admin & Finance', 'ACTIVE', 'admin', 'admin');


-- =============================================
-- DEVICES
-- =============================================
-- Tenant 1 Devices
INSERT INTO device (uuid, tenant_id, group_id, name, location, serial_number, type,
                    status, license_status, created_by, updated_by)
SELECT
    'dev-' || printf('%08d', ROW_NUMBER() OVER (ORDER BY g.id)),
    1,
    g.id,
    'PC-' || substr(g.name,1,8) || '-' || printf('%03d', ROW_NUMBER() OVER (PARTITION BY g.id ORDER BY g.id)),
    'Room ' || printf('%03d', ROW_NUMBER() OVER (PARTITION BY g.id ORDER BY g.id)),
    'SN-' || printf('%012d', g.id * 100 + ROW_NUMBER() OVER (PARTITION BY g.id ORDER BY g.id)),
    'COMPUTER',
    'ACTIVE',
    'VALID',
    'admin',
    'admin'
FROM groups g
WHERE g.tenant_id = 1;

-- Tenant 2 Devices
INSERT INTO device (uuid, tenant_id, group_id, name, location, serial_number, type,
                    status, license_status, created_by, updated_by)
SELECT
    'dev-b-' || printf('%08d', ROW_NUMBER() OVER (ORDER BY g.id)),
    2,
    g.id,
    'Laptop-' || substr(g.name,1,8) || '-' || printf('%03d', ROW_NUMBER() OVER (PARTITION BY g.id ORDER BY g.id)),
    'Room ' || printf('%03d', ROW_NUMBER() OVER (PARTITION BY g.id ORDER BY g.id)),
    'SN-BETA-' || printf('%010d', g.id * 100 + ROW_NUMBER() OVER (PARTITION BY g.id ORDER BY g.id)),
    'COMPUTER',
    'ACTIVE',
    'VALID',
    'admin',
    'admin'
FROM groups g
WHERE g.tenant_id = 2;


-- =============================================
-- ALERTS + ALERT ACTIONS
-- =============================================

-- Real Critical Alert
INSERT INTO alert (uuid, device_id, tenant_id, message, status, severity, is_test, created_by)
VALUES ('alt-001', 1, 1, 'Duress button activated - Employee in distress',
        'TRIGGERED', 'CRITICAL', 0, 'system');

INSERT INTO alert_actions (tenant_id, alert_id, type, payload, notes, created_by)
VALUES
    (1, 1, 'TRIGGERED', '{}', 'Initial alert received', 'system'),
    (1, 1, 'ACKNOWLEDGED', '{}', 'Security team dispatched', 'security.admin'),
    (1, 1, 'RESOLVED', '{}', 'False alarm - accidental trigger', 'security.admin');

-- Second Real Alert
INSERT INTO alert (uuid, device_id, tenant_id, message, status, severity, is_test, created_by)
VALUES ('alt-002', 5, 1, 'Panic button pressed in Engineering Floor',
        'RESOLVED', 'CRITICAL', 0, 'system');

INSERT INTO alert_actions (tenant_id, alert_id, type, payload, notes, created_by)
VALUES
    (1, 2, 'TRIGGERED', '{}', NULL, 'system'),
    (1, 2, 'ACKNOWLEDGED', '{}', 'Team responded within 2 minutes', 'security.admin'),
    (1, 2, 'RESOLVED', '{}', 'Employee safe - medical issue', 'security.admin');

-- Test Alert
INSERT INTO alert (uuid, device_id, tenant_id, message, status, severity, is_test, test_reason, created_by)
VALUES ('alt-003', 28, 2, 'TEST: Monthly duress alarm drill',
        'RESOLVED', 'INFO', 1, 'Monthly Staff Training Drill', 'admin');

INSERT INTO alert_actions (tenant_id, alert_id, type, payload, notes, created_by)
VALUES
    (2, 3, 'TRIGGERED', '{}', 'Test initiated', 'admin'),
    (2, 3, 'RESOLVED', '{}', 'Test completed successfully - All systems working', 'admin');