-- Report query indexes: support the monthly manager reports, which filter by
-- time ranges and status on these frequently-aggregated tables.

-- Payments: monthly paid-amount SUM and paid/pending counts
ALTER TABLE payments
    ADD INDEX idx_payments_status_paid_at (status, paid_at),
    ADD INDEX idx_payments_created_at (created_at);

-- Contract deposits: monthly paid-deposit SUM
ALTER TABLE contract_deposits
    ADD INDEX idx_contract_deposits_status_paid_at (status, paid_at);

-- Contract renewals: monthly renewed-count
ALTER TABLE contract_renewals
    ADD INDEX idx_contract_renewals_renewed_at (renewed_at);

-- Rental contracts: monthly new / expired / terminated counts
ALTER TABLE rental_contracts
    ADD INDEX idx_rental_contracts_created_at (created_at),
    ADD INDEX idx_rental_contracts_status_end_date (status, end_date);

-- Maintenance requests: monthly status-grouped counts
ALTER TABLE maintenance_requests
    ADD INDEX idx_maintenance_requests_status_reported_at (status, reported_at);

-- Maintenance costs: monthly expense SUM
ALTER TABLE maintenance_costs
    ADD INDEX idx_maintenance_costs_created_at (created_at);