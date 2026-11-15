-- V59: Persist non-login property owners separately from authentication users.
-- Existing account-owned properties remain linked through owner_id. Admin-posted
-- properties for non-login owners use owner_record_id and leave owner_id NULL.

CREATE TABLE IF NOT EXISTS property_owners (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    email VARCHAR(320) NULL,
    phone VARCHAR(50) NULL,
    address VARCHAR(500) NULL,
    notes TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_property_owners_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE houses
    ADD COLUMN IF NOT EXISTS owner_record_id BIGINT UNSIGNED NULL AFTER owner_id,
    ADD INDEX idx_houses_owner_record_id (owner_record_id),
    ADD CONSTRAINT fk_houses_owner_record
        FOREIGN KEY (owner_record_id) REFERENCES property_owners(id)
        ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE rooms
    ADD COLUMN IF NOT EXISTS owner_record_id BIGINT UNSIGNED NULL AFTER owner_id,
    ADD INDEX idx_rooms_owner_record_id (owner_record_id),
    ADD CONSTRAINT fk_rooms_owner_record
        FOREIGN KEY (owner_record_id) REFERENCES property_owners(id)
        ON DELETE RESTRICT ON UPDATE CASCADE;
