-- Fix contract_tenants.tenant_id FK to reference tenants(id), not users(id).
-- The frontend and backend both treat ContractTenant.tenantId as the id of a
-- tenant profile (tenants table). The original FK pointed at users(id), so
-- inserting a tenant profile id failed the FK check and produced a 500.

ALTER TABLE contract_tenants DROP FOREIGN KEY fk_contract_tenants_tenant;

-- Remove any orphaned rows whose tenant_id is not a valid tenant profile id
-- (e.g. rows that were only ever insertable because the id coincidentally
-- matched a user id under the previous, incorrect FK).
DELETE ct FROM contract_tenants ct
    LEFT JOIN tenants t ON t.id = ct.tenant_id
    WHERE t.id IS NULL;

ALTER TABLE contract_tenants
    ADD CONSTRAINT fk_contract_tenants_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id)
        ON DELETE RESTRICT ON UPDATE CASCADE;
