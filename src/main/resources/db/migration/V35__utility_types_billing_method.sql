-- Utility types support two billing methods:
--   METERED - billed from meter readings (Water, Electricity)
--   FIXED   - billed as a flat monthly charge (Parking, Internet, Other)
--
-- Existing rows are defaulted to METERED, which matches the current Water /
-- Electricity usage, so this is a non-breaking, additive change.

ALTER TABLE utility_types
    ADD COLUMN billing_method ENUM('METERED', 'FIXED')
        NOT NULL DEFAULT 'METERED';

ALTER TABLE utility_types
    ADD INDEX idx_utility_types_billing_method (owner_id, billing_method);