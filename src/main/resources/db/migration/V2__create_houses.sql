CREATE TABLE houses (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    owner_id BIGINT UNSIGNED NOT NULL,

    name VARCHAR(200) NOT NULL,
    slug VARCHAR(200) NOT NULL,
    code VARCHAR(50) NOT NULL,

    address VARCHAR(500) NOT NULL,
    postal_code VARCHAR(20) NULL,

    latitude DECIMAL(10, 8) NULL,
    longitude DECIMAL(11, 8) NULL,

    total_rooms INT UNSIGNED NOT NULL DEFAULT 0,
    available_rooms INT UNSIGNED NOT NULL DEFAULT 0,

    description TEXT NULL,

    status ENUM('ACTIVE', 'INACTIVE', 'MAINTENANCE')
        NOT NULL DEFAULT 'ACTIVE',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_houses_owner
        FOREIGN KEY (owner_id)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    UNIQUE KEY uk_houses_owner_slug (owner_id, slug),
    UNIQUE KEY uk_houses_owner_code (owner_id, code),
    UNIQUE KEY uk_houses_owner_name (owner_id, name),

    UNIQUE KEY uk_houses_id_owner (id, owner_id),

    INDEX idx_houses_owner_id (owner_id),
    INDEX idx_houses_status (status)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;
