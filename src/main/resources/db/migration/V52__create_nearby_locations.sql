-- =====================================================================
-- V52: House-level nearby locations.
--
-- Each row describes a point of interest close to a house (school,
-- university, hospital, mall, market, restaurant, bank, tourist spot,
-- bus stop or other). A house can have zero or more nearby locations.
-- Deleting a house removes its nearby locations (child data).
-- =====================================================================

CREATE TABLE nearby_locations (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    house_id BIGINT UNSIGNED NOT NULL,

    name VARCHAR(200) NOT NULL,
    category ENUM('SCHOOL', 'UNIVERSITY', 'HOSPITAL', 'MALL', 'MARKET',
                  'RESTAURANT', 'BANK', 'TOURIST_SPOT', 'BUS_STOP', 'OTHER')
        NOT NULL,
    distance_km DECIMAL(10, 2) NOT NULL,
    travel_minutes INT NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_nearby_locations_house
        FOREIGN KEY (house_id)
        REFERENCES houses(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    INDEX idx_nearby_locations_house_id (house_id)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;