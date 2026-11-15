-- =====================================================================
-- Seed the full permission catalog and assign it to the system roles.
--
-- Non-destructive and idempotent:
--   * permissions are upserted on their unique `code` (updates name/module/description only)
--   * role_permissions assignments are INSERT IGNORE (never removes existing grants)
--   * roles that do not exist in a given database are simply skipped
--
-- Role mapping:
--   OWNER   -> every permission (full system access; also enforced in SecurityUtils)
--   MANAGER -> all business modules (HOUSE, ROOM, TENANT, CONTRACT, PAYMENT, INVOICE,
--              MAINTENANCE, EXPENSE, AMENITY, UTILITY, REPORT, DASHBOARD, NOTIFICATION,
--              ASSISTANT) + HOUSE_ACCESS_VIEW. NOT the USER or SETTING modules.
--   TENANT  -> self-service permissions only.
-- =====================================================================

-- 1) Permission catalog (MODULE_ACTION naming).
INSERT INTO permissions (name, code, module, description, created_at) VALUES
  -- HOUSE
  ('View House',         'HOUSE_VIEW',         'HOUSE',         'Allows viewing houses',                          NOW()),
  ('Create House',       'HOUSE_CREATE',       'HOUSE',         'Allows creating new houses',                     NOW()),
  ('Update House',       'HOUSE_UPDATE',       'HOUSE',         'Allows updating house details',                  NOW()),
  ('Delete House',       'HOUSE_DELETE',       'HOUSE',         'Allows deleting houses',                          NOW()),
  ('Manage House',       'HOUSE_MANAGE',       'HOUSE',         'Allows creating, updating, and managing houses', NOW()),

  -- ROOM
  ('View Room',          'ROOM_VIEW',          'ROOM',          'Allows viewing rooms',                           NOW()),
  ('Create Room',        'ROOM_CREATE',        'ROOM',          'Allows creating new rooms',                      NOW()),
  ('Update Room',        'ROOM_UPDATE',        'ROOM',          'Allows updating room details',                   NOW()),
  ('Delete Room',        'ROOM_DELETE',        'ROOM',          'Allows deleting rooms',                           NOW()),
  ('Manage Room',        'ROOM_MANAGE',        'ROOM',          'Allows creating, updating, and managing rooms',  NOW()),

  -- TENANT
  ('View Tenant',        'TENANT_VIEW',        'TENANT',        'Allows viewing tenants',                          NOW()),
  ('Create Tenant',      'TENANT_CREATE',      'TENANT',        'Allows creating new tenants',                     NOW()),
  ('Update Tenant',      'TENANT_UPDATE',      'TENANT',        'Allows updating tenant details',                  NOW()),
  ('Delete Tenant',      'TENANT_DELETE',      'TENANT',        'Allows deleting tenants',                         NOW()),
  ('Manage Tenant',      'TENANT_MANAGE',      'TENANT',        'Allows creating, updating, and managing tenants', NOW()),

  -- CONTRACT
  ('View Contract',      'CONTRACT_VIEW',      'CONTRACT',      'Allows viewing rental contracts',                NOW()),
  ('Create Contract',    'CONTRACT_CREATE',    'CONTRACT',      'Allows creating new rental contracts',           NOW()),
  ('Update Contract',    'CONTRACT_UPDATE',    'CONTRACT',      'Allows updating rental contracts',               NOW()),
  ('Delete Contract',    'CONTRACT_DELETE',    'CONTRACT',      'Allows deleting rental contracts',               NOW()),
  ('Manage Contract',    'CONTRACT_MANAGE',    'CONTRACT',      'Allows creating, updating, and managing contracts', NOW()),

  -- PAYMENT
  ('View Payment',       'PAYMENT_VIEW',       'PAYMENT',       'Allows viewing payments',                         NOW()),
  ('Create Payment',     'PAYMENT_CREATE',     'PAYMENT',       'Allows recording new payments',                   NOW()),
  ('Manage Payment',     'PAYMENT_MANAGE',     'PAYMENT',       'Allows managing payments',                         NOW()),

  -- INVOICE
  ('View Invoice',       'INVOICE_VIEW',       'INVOICE',       'Allows viewing invoices',                         NOW()),
  ('Create Invoice',     'INVOICE_CREATE',     'INVOICE',       'Allows generating invoices',                      NOW()),
  ('Update Invoice',     'INVOICE_UPDATE',     'INVOICE',       'Allows updating invoice items',                   NOW()),
  ('Manage Invoice',     'INVOICE_MANAGE',     'INVOICE',       'Allows managing invoices',                         NOW()),

  -- MAINTENANCE
  ('View Maintenance',   'MAINTENANCE_VIEW',   'MAINTENANCE',   'Allows viewing maintenance requests',             NOW()),
  ('Create Maintenance', 'MAINTENANCE_CREATE', 'MAINTENANCE',   'Allows creating maintenance requests',            NOW()),
  ('Update Maintenance', 'MAINTENANCE_UPDATE', 'MAINTENANCE',   'Allows updating maintenance requests',            NOW()),
  ('Delete Maintenance', 'MAINTENANCE_DELETE', 'MAINTENANCE',   'Allows deleting maintenance comments and costs',  NOW()),
  ('Manage Maintenance', 'MAINTENANCE_MANAGE', 'MAINTENANCE',   'Allows managing maintenance',                      NOW()),

  -- EXPENSE
  ('View Expense',       'EXPENSE_VIEW',       'EXPENSE',       'Allows viewing expenses',                         NOW()),
  ('Create Expense',     'EXPENSE_CREATE',     'EXPENSE',       'Allows creating new expenses',                    NOW()),
  ('Update Expense',     'EXPENSE_UPDATE',     'EXPENSE',       'Allows updating expenses',                        NOW()),
  ('Delete Expense',     'EXPENSE_DELETE',     'EXPENSE',       'Allows deleting expenses',                        NOW()),
  ('Manage Expense',     'EXPENSE_MANAGE',     'EXPENSE',       'Allows creating, updating, and managing expenses', NOW()),

  -- AMENITY
  ('View Amenity',       'AMENITY_VIEW',       'AMENITY',       'Allows viewing amenities',                        NOW()),
  ('Create Amenity',     'AMENITY_CREATE',     'AMENITY',       'Allows creating new amenities',                   NOW()),
  ('Update Amenity',     'AMENITY_UPDATE',     'AMENITY',       'Allows updating amenities',                       NOW()),
  ('Delete Amenity',     'AMENITY_DELETE',     'AMENITY',       'Allows deleting amenities',                       NOW()),
  ('Manage Amenity',     'AMENITY_MANAGE',     'AMENITY',       'Allows creating, updating, and managing amenities', NOW()),

  -- UTILITY
  ('View Utility',       'UTILITY_VIEW',       'UTILITY',       'Allows viewing utility data',                     NOW()),
  ('Create Utility',     'UTILITY_CREATE',     'UTILITY',       'Allows creating utility records',                 NOW()),
  ('Update Utility',     'UTILITY_UPDATE',     'UTILITY',       'Allows updating utility records',                 NOW()),
  ('Manage Utility',     'UTILITY_MANAGE',     'UTILITY',       'Allows managing utility data',                    NOW()),

  -- USER
  ('View User',          'USER_VIEW',          'USER',          'Allows viewing users',                            NOW()),
  ('Create User',        'USER_CREATE',        'USER',          'Allows creating new users',                       NOW()),
  ('Update User',        'USER_UPDATE',        'USER',          'Allows updating users',                           NOW()),
  ('Delete User',        'USER_DELETE',        'USER',          'Allows deleting users',                           NOW()),
  ('Manage User',        'USER_MANAGE',        'USER',          'Allows creating, updating, and managing users',   NOW()),

  -- REPORT
  ('View Report',        'REPORT_VIEW',        'REPORT',        'Allows viewing reports',                          NOW()),
  ('Manage Report',      'REPORT_MANAGE',      'REPORT',        'Allows managing reports',                         NOW()),

  -- DASHBOARD
  ('View Dashboard',     'DASHBOARD_VIEW',     'DASHBOARD',     'Allows viewing the dashboards',                   NOW()),

  -- SETTING
  ('View Setting',       'SETTING_VIEW',       'SETTING',       'Allows viewing settings',                         NOW()),
  ('Update Setting',     'SETTING_UPDATE',     'SETTING',       'Allows updating settings',                        NOW()),
  ('Manage Setting',     'SETTING_MANAGE',     'SETTING',       'Allows managing settings',                        NOW()),

  -- ASSISTANT
  ('View Assistant',     'ASSISTANT_VIEW',     'ASSISTANT',     'Allows using the AI assistant',                   NOW()),
  ('Manage Assistant',   'ASSISTANT_MANAGE',   'ASSISTANT',     'Allows managing the AI assistant',                NOW()),

  -- NOTIFICATION
  ('View Notification',  'NOTIFICATION_VIEW',  'NOTIFICATION',  'Allows viewing notifications',                    NOW()),
  ('Manage Notification','NOTIFICATION_MANAGE','NOTIFICATION',  'Allows managing notifications',                   NOW()),

  -- HOUSE ACCESS
  ('View House Access',  'HOUSE_ACCESS_VIEW',  'HOUSE_ACCESS',  'Allows viewing house access assignments',         NOW()),
  ('Manage House Access','HOUSE_ACCESS_MANAGE','HOUSE_ACCESS',  'Allows managing house access assignments',        NOW())

ON DUPLICATE KEY UPDATE
    name        = VALUES(name),
    module      = VALUES(module),
    description = VALUES(description);

-- 2) OWNER -> every permission.
INSERT IGNORE INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'OWNER';

-- 3) MANAGER -> all business modules (+ HOUSE_ACCESS_VIEW only for house access).
INSERT IGNORE INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'MANAGER'
  AND (p.module IN ('HOUSE', 'ROOM', 'TENANT', 'CONTRACT', 'PAYMENT', 'INVOICE',
                    'MAINTENANCE', 'EXPENSE', 'AMENITY', 'UTILITY', 'REPORT',
                    'DASHBOARD', 'NOTIFICATION', 'ASSISTANT')
       OR (p.module = 'HOUSE_ACCESS' AND p.code = 'HOUSE_ACCESS_VIEW'));

-- 4) TENANT -> self-service permissions only.
INSERT IGNORE INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'TENANT'
  AND p.code IN ('TENANT_VIEW', 'PAYMENT_VIEW', 'PAYMENT_CREATE', 'INVOICE_VIEW',
                 'MAINTENANCE_VIEW', 'MAINTENANCE_CREATE', 'DASHBOARD_VIEW',
                 'NOTIFICATION_VIEW', 'ASSISTANT_VIEW');