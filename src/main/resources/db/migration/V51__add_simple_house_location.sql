-- =====================================================================
-- V51: Simple house location — nullable administrative location fields.
--
-- Adds optional `city_province` and `google_maps_url` columns to `houses`
-- so an owner can describe where the property is without a structured
-- address model. Non-destructive: both columns are NULL, and existing
-- address/postal_code/latitude/longitude are left untouched.
-- =====================================================================

ALTER TABLE houses
    ADD COLUMN city_province VARCHAR(100) NULL DEFAULT NULL AFTER longitude,
    ADD COLUMN google_maps_url VARCHAR(1000) NULL DEFAULT NULL AFTER city_province;