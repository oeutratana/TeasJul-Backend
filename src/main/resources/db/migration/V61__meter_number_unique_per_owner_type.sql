-- Meter numbers must NOT be globally unique. Two different owners may use the same
-- meter number (e.g. both have an "EDC-A01" electricity meter), and the same owner may
-- reuse an identifier across different utility types (e.g. an electricity meter and a
-- water meter sharing a label).
--
-- Business scope: OWNER + UTILITY TYPE + METER NUMBER.
-- The existing global UNIQUE KEY uk_utility_meters_number (meter_number) is dropped and
-- replaced with a composite unique key scoped to the owner and utility type. Existing
-- rows remain untouched -- relaxing the scope cannot create duplicate-key conflicts.
ALTER TABLE utility_meters
    DROP INDEX uk_utility_meters_number,
    ADD UNIQUE KEY uk_utility_meters_owner_type_number
        (owner_id, utility_type_id, meter_number);