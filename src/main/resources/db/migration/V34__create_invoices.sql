CREATE TABLE invoices (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT UNSIGNED NOT NULL,
    invoice_no VARCHAR(50) NOT NULL,
    contract_id BIGINT UNSIGNED NOT NULL,
    billing_year SMALLINT NOT NULL,
    billing_month TINYINT NOT NULL,
    due_date DATE NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    discount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    late_fee DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,

    status ENUM('DRAFT', 'PENDING', 'PARTIAL', 'PAID', 'OVERDUE', 'CANCELLED')
        NOT NULL DEFAULT 'DRAFT',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_invoices_owner
        FOREIGN KEY (owner_id)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_invoices_contract
        FOREIGN KEY (contract_id)
        REFERENCES rental_contracts(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT uk_invoices_owner_no UNIQUE (owner_id, invoice_no),

    CONSTRAINT uk_invoices_contract_period
        UNIQUE (contract_id, billing_year, billing_month),

    INDEX idx_invoices_owner_id (owner_id),
    INDEX idx_invoices_contract_id (contract_id),
    INDEX idx_invoices_status (owner_id, status),
    INDEX idx_invoices_period (billing_year, billing_month),

    CONSTRAINT chk_invoices_month
        CHECK (billing_month BETWEEN 1 AND 12),

    CONSTRAINT chk_invoices_amounts
        CHECK (
            subtotal >= 0
            AND discount >= 0
            AND late_fee >= 0
            AND total_amount >= 0
        )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE invoice_items (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    invoice_id BIGINT UNSIGNED NOT NULL,

    item_type ENUM('RENT', 'ELECTRICITY', 'WATER', 'INTERNET', 'PARKING', 'OTHER')
        NOT NULL,

    description VARCHAR(255) NOT NULL,

    quantity DECIMAL(14,3) NOT NULL DEFAULT 1.000,
    unit_price DECIMAL(12,4) NOT NULL DEFAULT 0.0000,
    amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_invoice_items_invoice
        FOREIGN KEY (invoice_id)
        REFERENCES invoices(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    INDEX idx_invoice_items_invoice_id (invoice_id),

    CONSTRAINT chk_invoice_items_amounts
        CHECK (quantity >= 0 AND unit_price >= 0 AND amount >= 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;
