ALTER TABLE rental_contracts
    ADD COLUMN booking_id BIGINT UNSIGNED NULL,
    ADD CONSTRAINT fk_rental_contracts_booking
        FOREIGN KEY (booking_id) REFERENCES room_bookings(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    ADD INDEX idx_rental_contracts_booking (booking_id);
