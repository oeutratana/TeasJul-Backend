-- =====================================================================
-- V49: Room publication workflow foundation + SUPER_ADMIN approval role.
--
-- 1) Seeds the SUPER_ADMIN platform role idempotently. Reacts on the
--    unique roles.code using the same ON DUPLICATE KEY UPDATE idiom V46
--    uses for permissions, so re-runs never create a duplicate role.
--
-- 2) Seeds the approval permission for the listing publication workflow
--    (MODULE_ACTION naming) and maps it to SUPER_ADMIN. SUPER_ADMIN is
--    granted ONLY this permission here -- it does NOT inherit OWNER or
--    MANAGER grants. Existing OWNER/MANAGER/TENANT grants are untouched.
--
-- 3) Adds the publication workflow columns directly to `rooms`.
--    Publication status is a SEPARATE concept from rooms.status (the
--    availability model). A room is publicly listed only when:
--        rooms.status            = 'AVAILABLE'
--    AND rooms.publication_status = 'PUBLISHED'
--    Existing rows keep the column default DRAFT, so no existing room is
--    ever made publicly visible by this migration.
--
-- 4) Adds composite indexes for the two primary publication queries:
--      * admin review queue        : (publication_status, submitted_at)
--      * public listing projection : (publication_status, status)
--    Single-column indexes for rooms.status, rooms.house_id, and
--    rooms.owner_id already exist (created in v9_create_room.sql).
--
-- 5) submitted_at/approved_at/... audit columns reference users(id)
--    following the nullable audit-column FK convention already used in
--    V25 (ON DELETE SET NULL ON UPDATE CASCADE).
--
-- Non-destructive: never edits an existing migration, never alters
-- rooms.status or any existing column/index.
-- =====================================================================

-- 1) SUPER_ADMIN platform role (idempotent upsert on unique role code).
INSERT INTO roles (name, code, description, is_system_role, created_at, updated_at)
VALUES (
    'Super Admin',
    'SUPER_ADMIN',
    'Platform administrator with system-wide listing review and approval privileges.',
    1, NOW(), NOW()
)
ON DUPLICATE KEY UPDATE
    name           = VALUES(name),
    description    = VALUES(description),
    is_system_role = VALUES(is_system_role);

-- 2a) Approval permission for the listing publication workflow.
INSERT INTO permissions (name, code, module, description, created_at)
VALUES (
    'Approve Listing',
    'LISTING_APPROVE',
    'LISTING',
    'Allows reviewing and approving room listing publications',
    NOW()
)
ON DUPLICATE KEY UPDATE
    name        = VALUES(name),
    module      = VALUES(module),
    description = VALUES(description);

-- 2b) Map SUPER_ADMIN -> LISTING_APPROVE (INSERT IGNORE, never removes grants).
INSERT IGNORE INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'SUPER_ADMIN'
  AND p.code = 'LISTING_APPROVE';

-- 3) Room publication workflow columns (existing rows default to DRAFT).
ALTER TABLE rooms
    ADD COLUMN publication_status ENUM('DRAFT', 'PENDING_APPROVAL', 'PUBLISHED', 'REJECTED', 'UNPUBLISHED')
        NOT NULL DEFAULT 'DRAFT' AFTER status,
    ADD COLUMN submitted_at TIMESTAMP NULL DEFAULT NULL AFTER publication_status,
    ADD COLUMN submitted_by BIGINT UNSIGNED NULL DEFAULT NULL AFTER submitted_at,
    ADD COLUMN approved_at TIMESTAMP NULL DEFAULT NULL AFTER submitted_by,
    ADD COLUMN approved_by BIGINT UNSIGNED NULL DEFAULT NULL AFTER approved_at,
    ADD COLUMN published_at TIMESTAMP NULL DEFAULT NULL AFTER approved_by,
    ADD COLUMN rejected_at TIMESTAMP NULL DEFAULT NULL AFTER published_at,
    ADD COLUMN rejected_by BIGINT UNSIGNED NULL DEFAULT NULL AFTER rejected_at,
    ADD COLUMN rejection_reason VARCHAR(1000) NULL DEFAULT NULL AFTER rejected_by,

    -- 4) Publication query indexes.
    ADD INDEX idx_rooms_publication_submitted (publication_status, submitted_at),
    ADD INDEX idx_rooms_publication_room_status (publication_status, status),

    -- 5) Audit-column foreign keys (nullable -> ON DELETE SET NULL).
    ADD CONSTRAINT fk_rooms_submitted_by
        FOREIGN KEY (submitted_by)
        REFERENCES users(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,
    ADD CONSTRAINT fk_rooms_approved_by
        FOREIGN KEY (approved_by)
        REFERENCES users(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,
    ADD CONSTRAINT fk_rooms_rejected_by
        FOREIGN KEY (rejected_by)
        REFERENCES users(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE;