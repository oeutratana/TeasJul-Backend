ALTER TABLE rooms
    MODIFY COLUMN `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    MODIFY COLUMN `house_id` BIGINT UNSIGNED NOT NULL,
    MODIFY COLUMN `owner_id` BIGINT UNSIGNED NOT NULL;

ALTER TABLE rooms
    DROP INDEX IF EXISTS uk_rooms_id_owner,
    ADD UNIQUE KEY uk_rooms_id_owner (id, owner_id);

ALTER TABLE tenants
    DROP INDEX IF EXISTS uk_tenants_id_owner,
    ADD UNIQUE KEY uk_tenants_id_owner (id, owner_id);

CREATE TABLE maintenance_requests (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT UNSIGNED NOT NULL,
    request_no VARCHAR(50) NOT NULL,
    house_id BIGINT UNSIGNED NOT NULL,
    room_id BIGINT UNSIGNED NOT NULL,
    tenant_id BIGINT UNSIGNED NULL,
    category_id BIGINT UNSIGNED NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NULL,

    priority ENUM('LOW', 'MEDIUM', 'HIGH', 'URGENT')
        NOT NULL DEFAULT 'MEDIUM',

    status ENUM('PENDING', 'IN_PROGRESS', 'RESOLVED', 'CANCELLED')
        NOT NULL DEFAULT 'PENDING',

    reported_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_maintenance_requests_owner
        FOREIGN KEY (owner_id)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_maintenance_requests_house_owner
        FOREIGN KEY (house_id, owner_id)
        REFERENCES houses(id, owner_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_maintenance_requests_room_owner
        FOREIGN KEY (room_id, owner_id)
        REFERENCES rooms(id, owner_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_maintenance_requests_tenant_owner
        FOREIGN KEY (tenant_id, owner_id)
        REFERENCES tenants(id, owner_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_maintenance_requests_category_owner
        FOREIGN KEY (category_id, owner_id)
        REFERENCES maintenance_categories(id, owner_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    UNIQUE KEY uk_maintenance_requests_id_owner
        (id, owner_id),

    UNIQUE KEY uk_maintenance_requests_owner_no
        (owner_id, request_no),

    INDEX idx_maintenance_requests_owner_id
        (owner_id),

    INDEX idx_maintenance_requests_house_id
        (house_id),

    INDEX idx_maintenance_requests_room_id
        (room_id),

    INDEX idx_maintenance_requests_tenant_id
        (tenant_id),

    INDEX idx_maintenance_requests_status
        (status),

    INDEX idx_maintenance_requests_priority
        (priority)

) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;
