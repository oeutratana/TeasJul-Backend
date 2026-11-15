CREATE TABLE IF NOT EXISTS rooms (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    owner_id BIGINT UNSIGNED NOT NULL,
    house_id BIGINT UNSIGNED NOT NULL,
    room_type_id BIGINT UNSIGNED NOT NULL,
    room_number VARCHAR(50) NOT NULL,
    name VARCHAR(100) DEFAULT NULL,
    floor_number INT DEFAULT NULL,
    monthly_rent DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    security_deposit DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    capacity INT NOT NULL DEFAULT 1,
    status ENUM('AVAILABLE', 'OCCUPIED', 'MAINTENANCE', 'RESERVED') NOT NULL DEFAULT 'AVAILABLE',
    description TEXT DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    UNIQUE KEY uk_rooms_house_number (house_id, room_number),

    INDEX idx_rooms_owner_id (owner_id),
    INDEX idx_rooms_house_id (house_id),
    INDEX idx_rooms_room_type_id (room_type_id),
    INDEX idx_rooms_status (status),

    CONSTRAINT fk_rooms_room_type 
        FOREIGN KEY (room_type_id) 
        REFERENCES room_types(id) 
        ON DELETE RESTRICT 
        ON UPDATE CASCADE

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;