CREATE TABLE tenant (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        uuid TEXT NOT NULL UNIQUE, -- API-safe public identifier
                        name TEXT NOT NULL UNIQUE,
                        description TEXT,
                        status TEXT NOT NULL,
                        current_device_count INTEGER NOT NULL DEFAULT 0,
                        license_status TEXT DEFAULT 'VALID',
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        created_by TEXT NOT NULL,
                        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        updated_by TEXT NOT NULL,
                        CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'TRIAL', 'EXPIRED', 'PENDING'))

);

CREATE TABLE workplace (
                           id INTEGER PRIMARY KEY AUTOINCREMENT,
                           uuid TEXT NOT NULL UNIQUE,
                           tenant_id INTEGER NOT NULL,
                           name TEXT NOT NULL,
                           address TEXT,
                           status TEXT NOT NULL,
                           created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                           created_by TEXT NOT NULL,
                           updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                           updated_by TEXT NOT NULL,
                           FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
                           CHECK (status IN ('ACTIVE', 'INACTIVE', 'UNDER_MAINTENANCE', 'SUSPENDED', 'ARCHIVED'))
);
CREATE INDEX idx_workplaces_tenant ON workplace(id);


CREATE TABLE groups (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        uuid TEXT NOT NULL UNIQUE,
                        tenant_id INTEGER NOT NULL,
                        workplace_id INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        status TEXT NOT NULL,
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        created_by TEXT NOT NULL,
                        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        updated_by TEXT NOT NULL,
                        FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
                        FOREIGN KEY (workplace_id) REFERENCES workplace(id) ON DELETE CASCADE,
                        CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED', 'UNDER_MAINTENANCE'))

);
CREATE INDEX idx_groups_tenant ON groups(tenant_id);


CREATE TABLE device (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        uuid TEXT NOT NULL UNIQUE,
                        group_id INTEGER NOT NULL,
                        tenant_id INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        location TEXT NOT NULL, -- TODO: here we might need to create a master table to store building, room
                        serial_number TEXT NOT NULL UNIQUE,
                        type TEXT NOT NULL DEFAULT 'COMPUTER', -- device type
                        status TEXT NOT NULL DEFAULT 'ACTIVE',
                        license_status TEXT NOT NULL DEFAULT 'VALID',
                        license_status_updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        license_notes TEXT,
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        created_by TEXT NOT NULL,
                        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        updated_by TEXT NOT NULL,
                        FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
                        FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE,
                        CHECK (license_status IN ('VALID','OVER_LIMIT','GRACE_PERIOD','BLOCKED','INACTIVE','SUSPENDED','EXPIRED'))
);
CREATE INDEX idx_devices_tenant ON device(tenant_id);


CREATE TABLE alert (
                       id INTEGER PRIMARY KEY AUTOINCREMENT,
                       uuid TEXT NOT NULL UNIQUE,
                       device_id INTEGER NOT NULL,
                       tenant_id INTEGER NOT NULL,
                       message TEXT NOT NULL,
                       status TEXT NOT NULL DEFAULT 'TRIGGERED',
                       severity TEXT NOT NULL DEFAULT 'CRITICAL',
                       is_test BOOLEAN NOT NULL DEFAULT 0,                    -- Main flag
                       test_reason TEXT,   -- e.g. "Monthly drill", "Training session", "System test"
                       created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                       created_by TEXT,
                       updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                       FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
                       FOREIGN KEY (device_id) REFERENCES device(id) ON DELETE CASCADE,
                       CHECK (status IN ('TRIGGERED', 'ACKNOWLEDGED', 'RESOLVED', 'FALSE_ALARM', 'CANCELLED'))

);
CREATE INDEX idx_alerts_tenant ON alert(tenant_id);
CREATE INDEX idx_alerts_device ON alert(device_id);
CREATE INDEX idx_alerts_test ON alert(is_test);
CREATE INDEX idx_alerts_created_at ON alert(created_at);


CREATE TABLE alert_actions (
                               id INTEGER PRIMARY KEY AUTOINCREMENT,
                               tenant_id INTEGER NOT NULL,
                               alert_id INTEGER NOT NULL,
                               type TEXT NOT NULL,		--TRIGGERED, ACKNOWLEDGE, FALSE_ALARM, RESOLVED
                               payload TEXT NOT NULL,
                               notes TEXT,
                               created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                               created_by TEXT NOT NULL,
                               FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
                               FOREIGN KEY (alert_id) REFERENCES alert(id) ON DELETE CASCADE,
                               CHECK (type IN ('TRIGGERED','ACKNOWLEDGED','FALSE_ALARM','RESOLVED'))
);
CREATE INDEX idx_alert_actions_tenant ON alert_actions(tenant_id);


CREATE TABLE user (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      uuid TEXT NOT NULL UNIQUE,
                      tenant_id INTEGER NOT NULL,
                      username TEXT NOT NULL UNIQUE,
                      email TEXT NOT NULL UNIQUE,
                      first_name TEXT,
                      last_name TEXT,
                      phone TEXT,
                      status TEXT NOT NULL,
                      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                      created_by TEXT NOT NULL,
                      FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);
CREATE INDEX idx_users_tenant ON user(tenant_id);

CREATE TABLE user_role (
                           user_id INTEGER NOT NULL,
                           role TEXT NOT NULL,
                           PRIMARY KEY (user_id, role),
                           FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
                           CHECK (role IN ('DEVICE', 'MONITOR', 'ADMIN', 'SUPPORT'))
);

CREATE TABLE device_location_history (
                                         id INTEGER PRIMARY KEY AUTOINCREMENT,

                                         device_id INTEGER NOT NULL,
                                         tenant_id INTEGER NOT NULL,

    -- Current assignment (at the time)
                                         workplace_id INTEGER NOT NULL,
                                         group_id INTEGER NOT NULL,

                                         assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         unassigned_at DATETIME,                    -- NULL = currently active

                                         assigned_by TEXT NOT NULL,                 -- user who performed the move
                                         notes TEXT,

                                         FOREIGN KEY (device_id) REFERENCES device(id) ON DELETE CASCADE,
                                         FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
                                         FOREIGN KEY (workplace_id) REFERENCES workplace(id),
                                         FOREIGN KEY (group_id) REFERENCES groups(id),

                                         CHECK (unassigned_at IS NULL OR unassigned_at > assigned_at)
);
CREATE INDEX idx_device_history_device ON device_location_history(device_id);
CREATE INDEX idx_device_history_tenant ON device_location_history(tenant_id);
CREATE INDEX idx_device_history_current ON device_location_history(device_id) WHERE unassigned_at IS NULL;
CREATE INDEX idx_device_history_period ON device_location_history(assigned_at, unassigned_at);


CREATE TABLE subscription_plan (
                                   id INTEGER PRIMARY KEY AUTOINCREMENT,
                                   name TEXT NOT NULL UNIQUE,                    -- e.g. "Starter", "Professional", "Enterprise"
                                   description TEXT,

    -- === Seat Limits ===
                                   max_devices INTEGER NOT NULL,                 -- e.g. 10, 25, 50, 100 (0 = unlimited)
                                   max_users INTEGER NOT NULL DEFAULT 999999,    -- 0 = unlimited
                                   max_workplaces INTEGER DEFAULT 0,             -- 0 = unlimited

    -- === Billing ===
                                   billing_cycle TEXT NOT NULL DEFAULT 'MONTHLY', -- MONTHLY, YEARLY
                                   price_cents INTEGER NOT NULL,
                                   currency TEXT NOT NULL DEFAULT 'AUD',

    -- === Overage & Grace Policy ===
                                   allow_overage BOOLEAN NOT NULL DEFAULT FALSE,   -- Use INTEGER (0/1) in SQLite
                                   overage_grace_days INTEGER NOT NULL DEFAULT 14,

                                   features JSON,                                -- e.g. {"alert_retention_days": 365, "sso": true}

                                   status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'DEPRECATED')),
                                   created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                   updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- TENANT SUBSCRIPTIONS
-- =============================================
CREATE TABLE tenant_subscription (
                                     id INTEGER PRIMARY KEY AUTOINCREMENT,
                                     tenant_id INTEGER NOT NULL,
                                     plan_id INTEGER NOT NULL,
                                     status TEXT NOT NULL DEFAULT 'ACTIVE',        -- ACTIVE, TRIAL, PAST_DUE, CANCELLED, EXPIRED, SUSPENDED
                                     starts_at DATETIME NOT NULL,
                                     ends_at DATETIME,
                                     auto_renew BOOLEAN NOT NULL DEFAULT 1,
                                     cancelled_at DATETIME,

                                     created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                     created_by TEXT NOT NULL,
                                     updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                     updated_by TEXT NOT NULL,
                                     FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
                                     FOREIGN KEY (plan_id) REFERENCES subscription_plan(id)
);
CREATE UNIQUE INDEX idx_tenant_one_active_subscription
    ON tenant_subscription(tenant_id)
    WHERE status IN ('ACTIVE', 'TRIAL', 'PAST_DUE');

-- =============================================
-- SUBSCRIPTION HISTORY
-- =============================================
CREATE TABLE tenant_subscription_history (
                                             id INTEGER PRIMARY KEY AUTOINCREMENT,
                                             tenant_id INTEGER NOT NULL,
                                             tenant_subscription_id INTEGER NOT NULL,
                                             old_plan_id INTEGER,
                                             new_plan_id INTEGER,
                                             old_status TEXT,
                                             new_status TEXT,
                                             change_type TEXT NOT NULL,          -- UPGRADE, DOWNGRADE, RENEWAL, CANCEL, SUSPEND
                                             reason TEXT,
                                             changed_by TEXT NOT NULL,
                                             changed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                             FOREIGN KEY (tenant_subscription_id) REFERENCES tenant_subscription(id),
                                             FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);

-- Main License Status View
CREATE VIEW v_tenant_license AS
SELECT
    t.id AS tenant_id,
    ts.id AS subscription_id,
    p.name AS plan_name,
    p.max_devices,
    p.allow_overage,
    p.overage_grace_days,

    t.current_device_count,

    CASE
        WHEN ts.status NOT IN ('ACTIVE', 'TRIAL')
            OR (ts.ends_at IS NOT NULL AND ts.ends_at < CURRENT_TIMESTAMP)
            THEN 'EXPIRED'

        WHEN p.max_devices = 0 OR t.current_device_count <= p.max_devices
            THEN 'VALID'

        WHEN p.allow_overage = 1
            THEN 'OVER_LIMIT_GRACE'

        ELSE 'OVER_LIMIT_BLOCKED'
        END AS license_status
FROM tenant t
         LEFT JOIN tenant_subscription ts ON ts.tenant_id = t.id
    AND ts.status IN ('ACTIVE','TRIAL','PAST_DUE')
         LEFT JOIN subscription_plan p ON p.id = ts.plan_id;
