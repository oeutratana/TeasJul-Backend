-- Enforce one ACTIVE meter per (room, utility_type).
-- Meters assigned to a house without a room (room_id IS NULL) are not valid for
-- room-level utility billing, so deactivate them (keeping their historical readings).
UPDATE utility_meters
    SET status = 'INACTIVE'
    WHERE room_id IS NULL AND status = 'ACTIVE';

-- Build a partial unique index that constrains only ACTIVE meters:
--   active_room_key = room_id when the meter is ACTIVE, NULL otherwise.
-- MySQL unique indexes allow multiple NULLs, so INACTIVE/REPLACED historical
-- meters of the same type in the same room do not conflict with each other or
-- with an active meter. This enforces "one active meter per (room, utility_type)".
ALTER TABLE utility_meters
    ADD COLUMN active_room_key BIGINT UNSIGNED AS (
        CASE WHEN status = 'ACTIVE' THEN room_id ELSE NULL END
    ) VIRTUAL,
    ADD UNIQUE KEY uk_utility_meters_active_room_type
        (utility_type_id, active_room_key);

-- room_id remains NULLABLE: house-level meters (room_id IS NULL) are still valid
-- and retained for historical records; they are simply not constrained per room.
