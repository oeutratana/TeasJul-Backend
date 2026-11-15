CREATE TABLE room_utilities (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    owner_id        BIGINT UNSIGNED NOT NULL,
    room_id         BIGINT UNSIGNED NOT NULL,
    utility_type_id BIGINT UNSIGNED NOT NULL,

    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_room_utilities_owner
        FOREIGN KEY (owner_id)
        REFERENCES users (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_room_utilities_room
        FOREIGN KEY (room_id, owner_id)
        REFERENCES rooms (id, owner_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_room_utilities_type
        FOREIGN KEY (utility_type_id, owner_id)
        REFERENCES utility_types (id, owner_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT uk_room_utilities_room_type
        UNIQUE (room_id, utility_type_id),

    INDEX idx_room_utilities_room    (room_id),
    INDEX idx_room_utilities_type    (utility_type_id),
    INDEX idx_room_utilities_owner   (owner_id, status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;