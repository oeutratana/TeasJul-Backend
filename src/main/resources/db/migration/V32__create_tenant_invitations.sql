-- Secure tenant invitation / onboarding flow.
--
-- 1) A manager/owner issues a short-lived invitation for a house they manage.
-- 2) The raw token travels only in the create response (and the link/QR the
--    frontend builds from it). Only the SHA-256 digest is stored here.
-- 3) Tenants open the public invite page, validate the token, and complete
--    registration. On acceptance a Tenant profile is created with status
--    PENDING; the manager assigns house/room/contract later (ACTIVE).
--
-- The tenants.status column is widened to include PENDING so onboarding
-- creates a profile that is visible but not yet renting.

ALTER TABLE tenants
    MODIFY COLUMN status ENUM('ACTIVE', 'RENTING', 'INACTIVE', 'PENDING')
        NOT NULL DEFAULT 'ACTIVE';

CREATE TABLE tenant_invitations (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    -- The authenticated OWNER/MANAGER user who created the invitation.
    manager_id BIGINT UNSIGNED NOT NULL,

    -- House the invited tenant will join. Required: on acceptance the tenant
    -- profile is bound to this house's owner.
    house_id BIGINT UNSIGNED NOT NULL,

    -- Optional. When set, only accounts using this email may accept.
    email VARCHAR(150) NULL,

    -- SHA-256 hex digest of the raw one-time token (never stored raw).
    token_hash VARCHAR(64) NOT NULL,

    -- Invitations expire 5 minutes after creation (server-side check only).
    expires_at DATETIME NOT NULL,

    used_at DATETIME NULL,

    status ENUM('PENDING', 'USED', 'EXPIRED', 'REVOKED')
        NOT NULL DEFAULT 'PENDING',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_tenant_invitations_manager
        FOREIGN KEY (manager_id)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_tenant_invitations_house
        FOREIGN KEY (house_id)
        REFERENCES houses(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    UNIQUE KEY uk_tenant_invitations_token_hash (token_hash),

    INDEX idx_tenant_invitations_manager_id (manager_id),
    INDEX idx_tenant_invitations_house_id (house_id),
    INDEX idx_tenant_invitations_status (status),
    INDEX idx_tenant_invitations_expires_at (expires_at)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;