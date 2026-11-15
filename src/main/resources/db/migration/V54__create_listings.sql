-- =====================================================================
-- V54: Grouped multi-room Listings
-- =====================================================================
-- A Listing is a first-class public presentation representing MULTIPLE
-- physical Rooms of the SAME RoomType under one House. The owner publishes
-- ONE listing (per house + roomType) instead of posting each room as a
-- separate listing.
--
--   listings          <- one row per (house_id, room_type_id)
--   rooms.listing_id  <- optional link from each physical room to its listing
--   listing_images    <- listing-level gallery (reuses Cloudinary pattern)
--   listing_utilities <- shared utility configuration for the group
--   listing_amenities <- shared amenity configuration for the group
--   listings.blueprint_*  <- floor plan / blueprint (single Cloudinary asset)
--
-- The existing Room publication workflow (rooms.publication_status) is
-- retained: submitting a listing moves the LISTING to PENDING_APPROVAL, and
-- approval publishes the listing AND its attached rooms together.
-- =====================================================================

CREATE TABLE IF NOT EXISTS listings (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    owner_id BIGINT UNSIGNED NOT NULL,
    house_id BIGINT UNSIGNED NOT NULL,
    room_type_id BIGINT UNSIGNED NOT NULL,

    title VARCHAR(120) DEFAULT NULL,
    description TEXT DEFAULT NULL,

    property_type ENUM('ROOM', 'HOUSE', 'VILLA', 'APARTMENT', 'CONDO', 'OTHER') DEFAULT NULL,
    listing_scope ENUM('INDIVIDUAL_ROOM', 'MULTIPLE_ROOMS', 'ENTIRE_PROPERTY') NOT NULL DEFAULT 'MULTIPLE_ROOMS',
    furnishing ENUM('FULLY_FURNISHED', 'SEMI_FURNISHED', 'UNFURNISHED') DEFAULT NULL,

    available_from DATE DEFAULT NULL,
    advance_payment DECIMAL(12, 2) DEFAULT NULL,
    bedrooms INT DEFAULT NULL,
    bathrooms INT DEFAULT NULL,
    area_sqm DECIMAL(10, 2) DEFAULT NULL,
    security_deposit DECIMAL(12, 2) DEFAULT NULL,

    blueprint_public_id VARCHAR(255) DEFAULT NULL,
    blueprint_secure_url VARCHAR(1024) DEFAULT NULL,

    publication_status ENUM('DRAFT', 'PENDING_APPROVAL', 'PUBLISHED', 'REJECTED', 'UNPUBLISHED')
        NOT NULL DEFAULT 'DRAFT',

    submitted_at TIMESTAMP NULL DEFAULT NULL,
    submitted_by BIGINT UNSIGNED NULL DEFAULT NULL,
    approved_at TIMESTAMP NULL DEFAULT NULL,
    approved_by BIGINT UNSIGNED NULL DEFAULT NULL,
    published_at TIMESTAMP NULL DEFAULT NULL,
    rejected_at TIMESTAMP NULL DEFAULT NULL,
    rejected_by BIGINT UNSIGNED NULL DEFAULT NULL,
    rejection_reason VARCHAR(1000) NULL DEFAULT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uk_listings_house_room_type (house_id, room_type_id),
    INDEX idx_listings_owner_id (owner_id),
    INDEX idx_listings_publication_status (publication_status),

    CONSTRAINT fk_listings_house
        FOREIGN KEY (house_id) REFERENCES houses(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_listings_room_type
        FOREIGN KEY (room_type_id) REFERENCES room_types(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_listings_submitted_by
        FOREIGN KEY (submitted_by) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_listings_approved_by
        FOREIGN KEY (approved_by) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_listings_rejected_by
        FOREIGN KEY (rejected_by) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Link physical rooms to their grouped listing.
-- ---------------------------------------------------------------------
ALTER TABLE rooms
    ADD COLUMN listing_id BIGINT UNSIGNED NULL DEFAULT NULL AFTER number_of_floors,
    ADD INDEX idx_rooms_listing_id (listing_id),
    ADD CONSTRAINT fk_rooms_listing
        FOREIGN KEY (listing_id) REFERENCES listings(id)
        ON DELETE SET NULL ON UPDATE CASCADE;

-- ---------------------------------------------------------------------
-- Listing gallery (same shape as house_images / room_type_images).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS listing_images (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    listing_id BIGINT UNSIGNED NOT NULL,
    public_id VARCHAR(255) NOT NULL,
    secure_url VARCHAR(1024) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_listing_images_listing FOREIGN KEY (listing_id) REFERENCES listings(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    UNIQUE KEY uk_listing_images_listing_public (listing_id, public_id),
    INDEX idx_listing_images_listing_id (listing_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Shared amenities configured at the listing level.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS listing_amenities (
    listing_id BIGINT UNSIGNED NOT NULL,
    amenity_id BIGINT UNSIGNED NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (listing_id, amenity_id),
    CONSTRAINT fk_listing_amenities_listing FOREIGN KEY (listing_id) REFERENCES listings(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_listing_amenities_amenity FOREIGN KEY (amenity_id) REFERENCES amenities(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_listing_amenities_listing_id (listing_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Shared utilities configured at the listing level.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS listing_utilities (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    owner_id BIGINT UNSIGNED NOT NULL,
    listing_id BIGINT UNSIGNED NOT NULL,
    utility_type_id BIGINT UNSIGNED NOT NULL,
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    is_included BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_listing_utilities_listing_type (listing_id, utility_type_id),
    CONSTRAINT fk_listing_utilities_owner FOREIGN KEY (owner_id) REFERENCES users(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_listing_utilities_listing FOREIGN KEY (listing_id) REFERENCES listings(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_listing_utilities_type FOREIGN KEY (utility_type_id) REFERENCES utility_types(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_listing_utilities_listing_id (listing_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- LISTING_VIEW / LISTING_MANAGE permissions for owner/manager flows
-- (idempotent, mirrors V46/V49 seeding).
-- ---------------------------------------------------------------------
INSERT INTO permissions (name, code, module, description, created_at)
VALUES (
    'Manage Listings', 'LISTING_MANAGE', 'LISTING',
    'Allows owners to create and manage grouped multi-room listings', NOW()
)
ON DUPLICATE KEY UPDATE
    name        = VALUES(name),
    module      = VALUES(module),
    description = VALUES(description);

INSERT IGNORE INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('SUPER_ADMIN', 'OWNER', 'MANAGER') AND p.code = 'LISTING_MANAGE';

INSERT INTO permissions (name, code, module, description, created_at)
VALUES (
    'View Listings', 'LISTING_VIEW', 'LISTING',
    'Allows viewing grouped multi-room listings', NOW()
)
ON DUPLICATE KEY UPDATE
    name        = VALUES(name),
    module      = VALUES(module),
    description = VALUES(description);

INSERT IGNORE INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('SUPER_ADMIN', 'OWNER', 'MANAGER') AND p.code = 'LISTING_VIEW';