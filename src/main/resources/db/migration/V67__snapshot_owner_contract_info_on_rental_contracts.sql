-- V67: Snapshot the owner's contract Party A identity onto each rental contract.
--
-- Party A is the owner. Previously the contract stored only owner_id, so every
-- document re-read the owner's current details at view time and a later settings
-- change silently rewrote history. These nullable columns freeze the signer
-- identity at creation time: old contracts keep what was signed, new contracts
-- pick up the current settings.
--
-- Columns stay nullable so contracts created before this migration keep working
-- and simply have no snapshot.

ALTER TABLE rental_contracts
    ADD COLUMN IF NOT EXISTS owner_full_name VARCHAR(200) NULL AFTER owner_id,
    ADD COLUMN IF NOT EXISTS owner_gender VARCHAR(20) NULL AFTER owner_full_name,
    ADD COLUMN IF NOT EXISTS owner_date_of_birth DATE NULL AFTER owner_gender,
    ADD COLUMN IF NOT EXISTS owner_nationality VARCHAR(100) NULL AFTER owner_date_of_birth,
    ADD COLUMN IF NOT EXISTS owner_national_id VARCHAR(100) NULL AFTER owner_nationality,
    ADD COLUMN IF NOT EXISTS owner_address VARCHAR(255) NULL AFTER owner_national_id,
    ADD COLUMN IF NOT EXISTS owner_phone VARCHAR(30) NULL AFTER owner_address,
    ADD COLUMN IF NOT EXISTS owner_occupation VARCHAR(150) NULL AFTER owner_phone;
