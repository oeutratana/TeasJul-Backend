CREATE TABLE user_house_access (
    user_id BIGINT UNSIGNED NOT NULL,
    house_id BIGINT UNSIGNED NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, house_id),

    CONSTRAINT fk_user_house_access_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_user_house_access_house
        FOREIGN KEY (house_id)
        REFERENCES houses(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    INDEX idx_user_house_access_user_id (user_id),
    INDEX idx_user_house_access_house_id (house_id)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;
