CREATE TABLE utility_rates (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    owner_id BIGINT UNSIGNED NOT NULL,
    house_id BIGINT UNSIGNED NOT NULL,
    utility_type_id BIGINT UNSIGNED NOT NULL,

    rate DECIMAL(12,4) NOT NULL,

    effective_from DATE NOT NULL,
    effective_to DATE NULL,

    status ENUM('ACTIVE', 'INACTIVE')
        NOT NULL DEFAULT 'ACTIVE',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_utility_rates_owner
        FOREIGN KEY (owner_id)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_utility_rates_house_owner
        FOREIGN KEY (house_id, owner_id)
        REFERENCES houses(id, owner_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_utility_rates_type_owner
        FOREIGN KEY (utility_type_id, owner_id)
        REFERENCES utility_types(id, owner_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    INDEX idx_utility_rates_owner_house_type
        (owner_id, house_id, utility_type_id),

    INDEX idx_utility_rates_effective
        (house_id, utility_type_id, effective_from),

    INDEX idx_utility_rates_status
        (owner_id, status),

    CONSTRAINT chk_utility_rates_rate
        CHECK (rate >= 0),

    CONSTRAINT chk_utility_rates_dates
        CHECK (
            effective_to IS NULL
            OR effective_to >= effective_from
        )
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;
