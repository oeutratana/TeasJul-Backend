-- The room_types.id column was created unsigned (v8_create_roomtype.sql), but a long-standing
-- Hibernate ddl-auto=update drift turned it into a signed bigint on existing databases.
-- Newer foreign keys (e.g. V48 room_type_images) expect the canonical unsigned type, so restore it.
-- No-op on schemas where it is already unsigned.
ALTER TABLE room_types
    MODIFY id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT;