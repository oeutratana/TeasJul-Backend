-- V68: Expense categories (owner-scoped).
-- Re-creates the expense_categories table that was previously defined only in the
-- unrecognized draft V33_create_expense_category.sql, so a pristine database can
-- initialize the application-managed schema. Idempotent: it will NOT reconcile an
-- existing table whose definition differs.
CREATE TABLE IF NOT EXISTS expense_categories (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    description VARCHAR(255) NULL,
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_expense_categories_owner
        FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE,

    UNIQUE KEY uk_expense_categories_owner_code (owner_id, code),
    UNIQUE KEY uk_expense_categories_owner_name (owner_id, name),
    -- Referenced by the composite owner-coupled FK from expenses (V69).
    UNIQUE KEY uk_expense_categories_id_owner (id, owner_id),
    INDEX idx_expense_categories_owner_id (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
