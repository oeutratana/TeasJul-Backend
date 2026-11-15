-- Track which manager (or owner) invited or created the tenant profile.
-- This allows house managers to see tenants they invited while isolating
-- them from tenants invited by other house managers.

ALTER TABLE tenants
    ADD COLUMN invited_by_id BIGINT UNSIGNED NULL AFTER house_id;

ALTER TABLE tenants
    ADD CONSTRAINT fk_tenants_invited_by
        FOREIGN KEY (invited_by_id)
        REFERENCES users(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE;

CREATE INDEX idx_tenants_invited_by_id ON tenants (invited_by_id);

-- Backfill any existing tenants that were registered via tenant invitations
UPDATE tenants t
JOIN users u ON t.user_id = u.id
JOIN tenant_invitations ti ON ti.email = u.email AND ti.house_id = t.house_id
SET t.invited_by_id = ti.manager_id
WHERE t.invited_by_id IS NULL;
