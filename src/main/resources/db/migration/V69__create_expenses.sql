-- V69: Expenses (owner-scoped, cross-owner isolated).
-- Re-creates the expenses table that was previously defined only in the
-- unrecognized draft V34_create_expense.sql, so a pristine database can initialize
-- the application-managed schema. Idempotent: it will NOT reconcile an existing
-- table whose definition differs.
--
-- NOTE: the legacy payment_method_id column and its foreign key are intentionally
-- omitted. The Expense entity no longer maps paymentMethod (commented out), and the
-- payment_methods table has no (id, owner_id) unique key, so the composite payment
-- FK used by the old draft could not be created anyway.
CREATE TABLE IF NOT EXISTS expenses (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT UNSIGNED NOT NULL,
    expense_no VARCHAR(50) NOT NULL,
    house_id BIGINT UNSIGNED NULL,
    category_id BIGINT UNSIGNED NOT NULL,
    description VARCHAR(255) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    expense_date DATE NOT NULL,
    status ENUM('PENDING', 'PAID', 'CANCELLED') NOT NULL DEFAULT 'PAID',
    created_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_expenses_owner
        FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    -- Ensures the referenced house belongs to the same owner as the expense.
    CONSTRAINT fk_expenses_house_owner
        FOREIGN KEY (house_id, owner_id) REFERENCES houses(id, owner_id) ON DELETE SET NULL ON UPDATE CASCADE,
    -- Ensures the referenced category belongs to the same owner as the expense.
    CONSTRAINT fk_expenses_category_owner
        FOREIGN KEY (category_id, owner_id) REFERENCES expense_categories(id, owner_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_expenses_created_by
        FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL ON UPDATE CASCADE,

    UNIQUE KEY uk_expenses_owner_no (owner_id, expense_no),
    INDEX idx_expenses_owner_id (owner_id),
    INDEX idx_expenses_house_id (house_id),
    INDEX idx_expenses_category_id (category_id),
    INDEX idx_expenses_date (expense_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
