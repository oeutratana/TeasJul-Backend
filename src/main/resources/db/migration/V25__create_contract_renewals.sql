CREATE TABLE contract_renewals (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    contract_id BIGINT UNSIGNED NOT NULL,
    previous_end_date DATE NULL,
    new_end_date DATE NOT NULL,
    previous_rent DECIMAL(12,2) NOT NULL,
    new_rent DECIMAL(12,2) NOT NULL,
    renewed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes TEXT NULL,
    created_by BIGINT UNSIGNED NULL,

    CONSTRAINT fk_contract_renewals_contract FOREIGN KEY (contract_id) REFERENCES rental_contracts(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_contract_renewals_user FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL ON UPDATE CASCADE,

    INDEX idx_contract_renewals_contract_id (contract_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
