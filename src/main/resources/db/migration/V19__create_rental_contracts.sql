CREATE TABLE rental_contracts (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT UNSIGNED NOT NULL,
    contract_no VARCHAR(50) NOT NULL,
    room_id BIGINT UNSIGNED NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NULL,
    monthly_rent DECIMAL(12, 2) NOT NULL,
    security_deposit DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    due_day SMALLINT NOT NULL DEFAULT 5,
    status ENUM('DRAFT', 'ACTIVE', 'TERMINATED', 'EXPIRED', 'CANCELLED')
        NOT NULL DEFAULT 'DRAFT',
    notes TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_contracts_owner
        FOREIGN KEY (owner_id)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT uk_contracts_owner_no UNIQUE (owner_id, contract_no),

    INDEX idx_contracts_owner_id (owner_id),
    INDEX idx_contracts_room_id (room_id),
    INDEX idx_contracts_status (status),
    INDEX idx_contracts_dates (start_date, end_date)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;
