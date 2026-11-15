-- =====================================================================
-- V56: Owner system feedback / testimonial table + permission seed.
--
-- 1) Seeds the three feedback permissions idempotently into the
--    permissions catalog using the same ON DUPLICATE KEY UPDATE idiom
--    established in V46.
--
-- 2) Grants FEEDBACK_MANAGE to SUPER_ADMIN so the admin review workflow
--    is visible in the permission catalog. OWNER implicitly holds every
--    permission via SecurityUtils.hasPermission and does NOT need an
--    explicit role_permissions row.
--
-- 3) Creates the feedbacks table. Each row represents a single
--    feedback/testimonial submission from an OWNER about the TeasJul
--    system. The rating column is constrained to 1-5 by the request
--    DTO and by a MySQL CHECK constraint below.
--
--    status lifecycle: PENDING -> PUBLISHED | UNPUBLISHED
--    (SUPER_ADMIN publish/unpublish, task-defined state machine).
--
-- Non-destructive: idempotent inserts only; new table does not
-- affect any existing table or column.
-- =====================================================================

-- 1) Feedback permissions (idempotent upsert on unique code).
INSERT INTO permissions (name, code, module, description, created_at)
VALUES
    ('Create Feedback', 'FEEDBACK_CREATE', 'FEEDBACK',
     'Allows owners to submit feedback about the TeasJul system', NOW()),
    ('View Feedback',   'FEEDBACK_VIEW',   'FEEDBACK',
     'Allows viewing own or public feedback submissions', NOW()),
    ('Manage Feedback', 'FEEDBACK_MANAGE', 'FEEDBACK',
     'Allows reviewing, publishing and unpublishing feedback', NOW())
ON DUPLICATE KEY UPDATE
    name        = VALUES(name),
    module      = VALUES(module),
    description = VALUES(description);

-- 2) Grant FEEDBACK_MANAGE to SUPER_ADMIN (idempotent INSERT IGNORE).
INSERT IGNORE INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'SUPER_ADMIN'
  AND p.code = 'FEEDBACK_MANAGE';

-- 3) Create feedbacks table.
CREATE TABLE feedbacks (
    id          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    owner_id    BIGINT UNSIGNED  NOT NULL,
    rating      INT              NOT NULL,
    comment     VARCHAR(1000)    NOT NULL,
    status      ENUM('PENDING','PUBLISHED','UNPUBLISHED')
                                  NOT NULL DEFAULT 'PENDING',
    reviewed_at TIMESTAMP        NULL DEFAULT NULL,
    reviewed_by BIGINT UNSIGNED  NULL DEFAULT NULL,
    created_at  TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    -- Rating bounded by application DTO; CHECK enforces it at the DB level.
    CONSTRAINT chk_feedbacks_rating CHECK (rating BETWEEN 1 AND 5),

    -- FK: owner row must exist.
    CONSTRAINT fk_feedbacks_owner
        FOREIGN KEY (owner_id)
        REFERENCES users(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    -- FK: reviewer (nullable, SET NULL on delete).
    CONSTRAINT fk_feedbacks_reviewed_by
        FOREIGN KEY (reviewed_by)
        REFERENCES users(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,

    -- Queries:
    --   Owner list page   : (owner_id, created_at)
    --   Admin list/status : (status, created_at)
    --   Public Top-N      : (status, created_at) -- same index covers both
    INDEX idx_feedbacks_owner_created  (owner_id, created_at),
    INDEX idx_feedbacks_status_created (status,   created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;