-- V60: First-class business references for idempotent in-app notifications.
-- Existing notifications remain valid with nullable references.

ALTER TABLE notifications
    ADD COLUMN reference_type VARCHAR(50) NULL AFTER data,
    ADD COLUMN reference_id BIGINT UNSIGNED NULL AFTER reference_type,
    ADD INDEX idx_notifications_reference (reference_type, reference_id);
