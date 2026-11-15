-- =====================================================================
-- V50: Rental listing foundation — nullable listing fields.
--
-- Adds optional listing columns to `rooms` and `houses` so property
-- managers can enrich a rental listing (property type, furnishing,
-- availability, dimensions) WITHOUT changing existing behaviour.
--
-- Non-destructive: all new columns are NULL, nothing existing is altered.
-- =====================================================================

ALTER TABLE rooms
    ADD COLUMN property_type ENUM('ROOM', 'HOUSE', 'VILLA', 'APARTMENT', 'CONDO', 'OTHER')
        NULL DEFAULT NULL AFTER description,
    ADD COLUMN listing_scope ENUM('INDIVIDUAL_ROOM', 'MULTIPLE_ROOMS', 'ENTIRE_PROPERTY')
        NULL DEFAULT NULL AFTER property_type,
    ADD COLUMN furnishing ENUM('FULLY_FURNISHED', 'SEMI_FURNISHED', 'UNFURNISHED')
        NULL DEFAULT NULL AFTER listing_scope,
    ADD COLUMN available_from DATE NULL DEFAULT NULL AFTER furnishing,
    ADD COLUMN advance_payment DECIMAL(12, 2) NULL DEFAULT NULL AFTER available_from,
    ADD COLUMN bedrooms INT NULL DEFAULT NULL AFTER advance_payment,
    ADD COLUMN bathrooms INT NULL DEFAULT NULL AFTER bedrooms,
    ADD COLUMN area_sqm DECIMAL(10, 2) NULL DEFAULT NULL AFTER bathrooms,
    ADD COLUMN land_size_sqm DECIMAL(10, 2) NULL DEFAULT NULL AFTER area_sqm,
    ADD COLUMN number_of_floors INT NULL DEFAULT NULL AFTER land_size_sqm;

ALTER TABLE houses
    ADD COLUMN property_type ENUM('ROOM', 'HOUSE', 'VILLA', 'APARTMENT', 'CONDO', 'OTHER')
        NULL DEFAULT NULL AFTER status,
    ADD COLUMN furnishing ENUM('FULLY_FURNISHED', 'SEMI_FURNISHED', 'UNFURNISHED')
        NULL DEFAULT NULL AFTER property_type,
    ADD COLUMN available_from DATE NULL DEFAULT NULL AFTER furnishing,
    ADD COLUMN advance_payment DECIMAL(12, 2) NULL DEFAULT NULL AFTER available_from,
    ADD COLUMN bedrooms INT NULL DEFAULT NULL AFTER advance_payment,
    ADD COLUMN bathrooms INT NULL DEFAULT NULL AFTER bedrooms,
    ADD COLUMN area_sqm DECIMAL(10, 2) NULL DEFAULT NULL AFTER bathrooms,
    ADD COLUMN land_size_sqm DECIMAL(10, 2) NULL DEFAULT NULL AFTER area_sqm,
    ADD COLUMN number_of_floors INT NULL DEFAULT NULL AFTER land_size_sqm;