-- Link a tenant profile to the house it belongs to.
--
-- Previously a tenant was only associated with a house through an ACTIVE
-- rental contract (ContractTenant -> RentalContract -> Room -> House). A tenant
-- accepted via an invitation has no contract yet, so a manager could not see
-- invited tenants on their assigned houses.
--
-- Adding house_id gives an explicit house linkage (set when a tenant accepts an
-- invitation) so that managers and owners can scope invited tenants by house.

ALTER TABLE tenants
    ADD COLUMN house_id BIGINT UNSIGNED NULL AFTER owner_id;

ALTER TABLE tenants
    ADD CONSTRAINT fk_tenants_house
        FOREIGN KEY (house_id)
        REFERENCES houses(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE;

CREATE INDEX idx_tenants_house_id ON tenants (house_id);