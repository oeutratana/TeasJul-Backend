CREATE TABLE owner_settings (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    owner_id        BIGINT UNSIGNED NOT NULL,
    settings        MEDIUMTEXT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_owner_settings_owner UNIQUE (owner_id),
    CONSTRAINT fk_owner_settings_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE
);

ALTER TABLE invoices
    ADD COLUMN exchange_rate DECIMAL(12,4) NULL AFTER total_amount,
    ADD COLUMN exchange_rate_currency VARCHAR(3) NULL AFTER exchange_rate;

ALTER TABLE payments
    ADD COLUMN exchange_rate DECIMAL(12,4) NULL AFTER currency,
    ADD COLUMN exchange_rate_to VARCHAR(3) NULL AFTER exchange_rate;
