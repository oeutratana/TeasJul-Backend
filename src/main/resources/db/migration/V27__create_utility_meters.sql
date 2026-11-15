ALTER TABLE rooms
    ADD UNIQUE KEY uk_rooms_id_house (id, house_id);

CREATE TABLE utility_meters (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    owner_id BIGINT UNSIGNED NOT NULL,
    house_id BIGINT UNSIGNED NOT NULL,
    room_id BIGINT UNSIGNED NULL,
    utility_type_id BIGINT UNSIGNED NOT NULL,

    meter_number VARCHAR(100) NOT NULL,

    installed_at DATE NULL,
    status ENUM('ACTIVE', 'INACTIVE', 'REPLACED')
        NOT NULL DEFAULT 'ACTIVE',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_utility_meters_owner
        FOREIGN KEY (owner_id)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_utility_meters_house_owner
        FOREIGN KEY (house_id, owner_id)
        REFERENCES houses(id, owner_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_utility_meters_type_owner
        FOREIGN KEY (utility_type_id, owner_id)
        REFERENCES utility_types(id, owner_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_utility_meters_room_house
        FOREIGN KEY (room_id, house_id)
        REFERENCES rooms(id, house_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    UNIQUE KEY uk_utility_meters_number (meter_number),

    INDEX idx_utility_meters_owner_house
        (owner_id, house_id),

    INDEX idx_utility_meters_room
        (room_id),

    INDEX idx_utility_meters_type
        (utility_type_id),

    INDEX idx_utility_meters_status
        (owner_id, status)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;
