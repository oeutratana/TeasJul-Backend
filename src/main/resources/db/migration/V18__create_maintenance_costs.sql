CREATE TABLE maintenance_costs (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    owner_id BIGINT UNSIGNED NOT NULL,
    request_id BIGINT UNSIGNED NOT NULL,

    cost_type ENUM(
        'LABOR',
        'SPARE_PART',
        'TRANSPORT',
        'MATERIAL',
        'OTHER'
    ) NOT NULL DEFAULT 'OTHER',

    description VARCHAR(255) NOT NULL,

    quantity DECIMAL(12,3) NOT NULL DEFAULT 1.000,
    unit_price DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_maintenance_costs_owner
        FOREIGN KEY (owner_id)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_maintenance_costs_request_owner
        FOREIGN KEY (request_id, owner_id)
        REFERENCES maintenance_requests(id, owner_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    INDEX idx_maintenance_costs_owner_id
        (owner_id),

    INDEX idx_maintenance_costs_request_id
        (request_id)

) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;
