CREATE TABLE utility_readings (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    meter_id BIGINT UNSIGNED NOT NULL,

    previous_reading DECIMAL(14,3) NOT NULL DEFAULT 0.000,
    current_reading DECIMAL(14,3) NOT NULL,
    `usage` DECIMAL(14,3) NOT NULL DEFAULT 0.000,

    reading_date DATE NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_utility_readings_meter
        FOREIGN KEY (meter_id)
        REFERENCES utility_meters(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    UNIQUE KEY uk_meter_reading_date
        (meter_id, reading_date),

    INDEX idx_utility_readings_meter_date
        (meter_id, reading_date),

    INDEX idx_utility_readings_date
        (reading_date),

    CONSTRAINT chk_utility_readings_current
        CHECK (current_reading >= 0),

    CONSTRAINT chk_utility_readings_previous
        CHECK (previous_reading >= 0),

    CONSTRAINT chk_utility_readings_usage
        CHECK (`usage` >= 0)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;
