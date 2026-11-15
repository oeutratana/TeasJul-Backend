-- =====================================================================
-- V53: Room utility inclusion flag.
--
-- Adds `is_included` to `room_utilities`. When TRUE the utility is part
-- of the rent: it is shown as "Included" and is skipped during invoice
-- generation (no rate required, no invoice line produced). Non-destructive
-- and additive: existing records default to FALSE (billed normally).
-- =====================================================================

ALTER TABLE room_utilities
    ADD COLUMN is_included BOOLEAN NOT NULL DEFAULT FALSE;