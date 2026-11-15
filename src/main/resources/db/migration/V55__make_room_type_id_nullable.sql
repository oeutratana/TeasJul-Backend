-- Household/structural listings (HOUSE, VILLA, APARTMENT, CONDO) and single
-- ROOMs may be posted by SUPER_ADMIN without a room type. Relax the NOT NULL
-- constraint that was created in v9_create_room.sql so `rooms.room_type_id`
-- becomes optional. Hibernate ddl-auto=update will not alter nullability for us.
ALTER TABLE rooms
    MODIFY room_type_id BIGINT UNSIGNED DEFAULT NULL;