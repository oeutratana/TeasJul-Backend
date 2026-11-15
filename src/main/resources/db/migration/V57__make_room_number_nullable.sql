-- Structural listings (HOUSE/VILLA) posted by SUPER_ADMIN are stored as a
-- single `rooms` row but have no physical room number, and APARTMENT/CONDO
-- units use the letter/figure unit number. Relax the NOT NULL constraint
-- created in v9_create_room.sql so `rooms.room_number` becomes optional.
-- The MySQL unique key (house_id, room_number) still allows multiple NULLs.
-- Hibernate ddl-auto=update will not alter nullability for us.
ALTER TABLE rooms
    MODIFY room_number VARCHAR(50) DEFAULT NULL;
