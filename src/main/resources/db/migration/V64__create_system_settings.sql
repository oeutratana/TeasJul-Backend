CREATE TABLE system_settings (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    setting_key     VARCHAR(100) NOT NULL UNIQUE,
    setting_value   MEDIUMTEXT NOT NULL,
    description     VARCHAR(255) NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_system_settings_key (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO system_settings (setting_key, setting_value, description)
VALUES (
    'SYSTEM_NOTIFICATION_SETTINGS',
    '{"systemNotificationsEnabled":true,"inAppNotificationsEnabled":true,"telegramNotificationsEnabled":true,"propertySubmittedEnabled":true,"invoiceCreatedEnabled":true,"paymentCreatedEnabled":true,"paymentSuccessEnabled":true,"paymentFailedEnabled":true,"maintenanceRequestEnabled":true,"maintenanceStatusEnabled":true,"contractExpiringEnabled":true,"roomAssignedEnabled":true}',
    'System-wide notification preferences'
);
