-- 10.2 EXPENSES (With cross-owner isolation)
CREATE TABLE expenses (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT UNSIGNED NOT NULL,
    expense_no VARCHAR(50) NOT NULL,
    house_id BIGINT UNSIGNED NULL,
    category_id BIGINT UNSIGNED NOT NULL,
    description VARCHAR(255) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    expense_date DATE NOT NULL,
    payment_method_id BIGINT UNSIGNED NULL,
    status ENUM('PENDING', 'PAID', 'CANCELLED') NOT NULL DEFAULT 'PAID',
    created_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_expenses_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    -- 🔥 Ensures house belongs to same owner
    CONSTRAINT fk_expenses_house_owner FOREIGN KEY (house_id, owner_id) REFERENCES houses(id, owner_id) ON DELETE SET NULL ON UPDATE CASCADE,
    -- 🔥 Ensures category belongs to same owner
    CONSTRAINT fk_expenses_category_owner FOREIGN KEY (category_id, owner_id) REFERENCES expense_categories(id, owner_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    -- 🔥 Ensures payment method belongs to same owner
    CONSTRAINT fk_expenses_payment_method_owner FOREIGN KEY (payment_method_id, owner_id) REFERENCES payment_methods(id, owner_id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_expenses_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL ON UPDATE CASCADE,
    
    UNIQUE KEY uk_expenses_owner_no (owner_id, expense_no),
    INDEX idx_expenses_owner_id (owner_id),
    INDEX idx_expenses_house_id (house_id),
    INDEX idx_expenses_category_id (category_id),
    INDEX idx_expenses_date (expense_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;