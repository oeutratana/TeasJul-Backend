CREATE TABLE house_images (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    house_id BIGINT UNSIGNED NOT NULL,
    public_id VARCHAR(255) NOT NULL,
    secure_url VARCHAR(1024) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_house_images_house
        FOREIGN KEY (house_id)
        REFERENCES houses(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    UNIQUE KEY uk_house_images_house_public (house_id, public_id),

    INDEX idx_house_images_house_id (house_id)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;