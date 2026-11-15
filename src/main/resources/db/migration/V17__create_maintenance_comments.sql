CREATE TABLE maintenance_comments (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    owner_id BIGINT UNSIGNED NOT NULL,
    request_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NULL,

    comment TEXT NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_maintenance_comments_owner
        FOREIGN KEY (owner_id)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_maintenance_comments_request_owner
        FOREIGN KEY (request_id, owner_id)
        REFERENCES maintenance_requests(id, owner_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_maintenance_comments_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,

    INDEX idx_maintenance_comments_owner_id
        (owner_id),

    INDEX idx_maintenance_comments_request
        (request_id),

    INDEX idx_maintenance_comments_user
        (user_id)

) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;
