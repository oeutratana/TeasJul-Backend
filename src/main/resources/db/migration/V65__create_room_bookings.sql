ALTER TABLE rooms
    MODIFY COLUMN status ENUM('AVAILABLE', 'BOOKED', 'OCCUPIED', 'MAINTENANCE', 'RESERVED')
    NOT NULL DEFAULT 'AVAILABLE';

CREATE TABLE room_bookings (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT UNSIGNED NOT NULL,
    customer_name VARCHAR(100) NOT NULL,
    customer_phone VARCHAR(30) NOT NULL,
    customer_email VARCHAR(150) NULL,
    booking_date DATE NOT NULL,
    expected_move_in_date DATE NOT NULL,
    status ENUM('BOOKED', 'CANCELLED', 'COMPLETED') NOT NULL DEFAULT 'BOOKED',
    note TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_room_bookings_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_room_bookings_room_status (room_id, status),
    INDEX idx_room_bookings_expected_move_in (expected_move_in_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
