-- =====================================================================
-- V62: System Default Utilities & Amenities.
--
-- Two concepts:
--   1. system_defaults        - reusable, SUPER_ADMIN-managed definitions for
--                                both utilities (kind = 'UTILITY') and
--                                amenities (kind = 'AMENITY').
--   2. owner_system_defaults  - per-owner enablement of a system default.
--                                Enabling a UTILITY default materialises (or
--                                reuses by code) a real owner-scoped
--                                utility_types row and records its id here.
--                                The same applies to amenities via amenities.
--
-- The owner-scoped copy keeps every existing flow working unchanged:
-- rooms use room_utilities.utility_type_id, meters/rates/readings/invoices
-- all key by utility_type_id/amenity_id and stay owner-scoped. No hardcoded
-- ids are introduced anywhere.
-- =====================================================================

CREATE TABLE system_defaults (
    id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    kind           ENUM('UTILITY', 'AMENITY') NOT NULL,

    name           VARCHAR(100) NOT NULL,
    code           VARCHAR(50)  NOT NULL,

    -- UTILITY-only
    unit           VARCHAR(30)  NULL,
    billing_method ENUM('METERED', 'FIXED') NULL,

    -- AMENITY-only
    description    TEXT         NULL,
    icon           VARCHAR(100) NULL,

    status         ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',

    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                       ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_system_defaults_kind_code (kind, code),
    UNIQUE KEY uk_system_defaults_kind_name (kind, name),

    INDEX idx_system_defaults_kind_status (kind, status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE owner_system_defaults (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    owner_id          BIGINT UNSIGNED NOT NULL,
    system_default_id BIGINT UNSIGNED NOT NULL,

    -- Materialised owner-scoped copy (nullable until the owner enables).
    utility_type_id   BIGINT UNSIGNED NULL,
    amenity_id        BIGINT UNSIGNED NULL,

    status            ENUM('ENABLED', 'DISABLED') NOT NULL DEFAULT 'ENABLED',

    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                          ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_owner_system_defaults_owner
        FOREIGN KEY (owner_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT fk_owner_system_defaults_default
        FOREIGN KEY (system_default_id) REFERENCES system_defaults (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT fk_owner_system_defaults_utility_type
        FOREIGN KEY (utility_type_id) REFERENCES utility_types (id)
        ON DELETE SET NULL ON UPDATE CASCADE,

    CONSTRAINT fk_owner_system_defaults_amenity
        FOREIGN KEY (amenity_id) REFERENCES amenities (id)
        ON DELETE SET NULL ON UPDATE CASCADE,

    UNIQUE KEY uk_owner_system_default (owner_id, system_default_id),

    INDEX idx_owner_system_defaults_owner_status (owner_id, status),
    INDEX idx_owner_system_defaults_default (system_default_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Canonical seed definitions. These are SYSTEM-provided defaults; Owners
-- opt in per account. Codes are matched case-insensitively against an
-- Owner's existing utility_types/amenities on enable, so pre-existing
-- Electricity/Water rows are reused instead of duplicated.
-- ---------------------------------------------------------------------

INSERT INTO system_defaults (kind, name, code, unit, billing_method, description, icon) VALUES
    ('UTILITY', 'Electricity', 'ELECTRIC', 'kWh', 'METERED', NULL, NULL),
    ('UTILITY', 'Water', 'WATER', 'm³', 'METERED', NULL, NULL),
    ('UTILITY', 'Garbage', 'GARBAGE', 'month', 'FIXED', NULL, NULL),
    ('UTILITY', 'Internet', 'INTERNET', 'month', 'FIXED', NULL, NULL),
    ('AMENITY', 'WiFi', 'WIFI', NULL, NULL, 'High-speed wireless internet', 'wifi'),
    ('AMENITY', 'Parking', 'PARKING', NULL, NULL, 'Parking space', 'car-park'),
    ('AMENITY', 'Swimming Pool', 'POOL', NULL, NULL, 'Shared swimming pool', 'pool'),
    ('AMENITY', 'Gym', 'GYM', NULL, NULL, 'Fitness gym access', 'dumbbell'),
    ('AMENITY', 'Air Conditioning', 'AIRCON', NULL, NULL, 'Air conditioning unit', 'snowflake'),
    ('AMENITY', 'Elevator', 'ELEVATOR', NULL, NULL, 'Elevator access', 'building');