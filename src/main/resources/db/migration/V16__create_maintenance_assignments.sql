CREATE TABLE maintenance_assignments (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    owner_id BIGINT UNSIGNED NOT NULL,
    request_id BIGINT UNSIGNED NOT NULL,
    assigned_to BIGINT UNSIGNED NOT NULL,

    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    notes TEXT NULL,

    CONSTRAINT fk_maintenance_assignments_owner
        FOREIGN KEY (owner_id)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_maintenance_assignments_request_owner
        FOREIGN KEY (request_id, owner_id)
        REFERENCES maintenance_requests(id, owner_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_maintenance_assignments_user
        FOREIGN KEY (assigned_to)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    INDEX idx_maintenance_assignments_owner_id
        (owner_id),

    INDEX idx_maintenance_assignments_request
        (request_id),

    INDEX idx_maintenance_assignments_user
        (assigned_to)

) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;
