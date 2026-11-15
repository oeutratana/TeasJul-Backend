CREATE TABLE tenants (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    -- The user account that represents this tenant.
    -- This user can log in to the system.
    user_id BIGINT UNSIGNED NOT NULL,

    -- The owner who manages/owns this tenant.
    owner_id BIGINT UNSIGNED NOT NULL,

    tenant_code VARCHAR(50) NOT NULL,

    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,

    gender ENUM('MALE', 'FEMALE', 'OTHER') NULL,

    date_of_birth DATE NULL,

    phone VARCHAR(30) NOT NULL,
    email VARCHAR(150) NULL,

    occupation_type ENUM('STUDENT', 'EMPLOYEE', 'OTHER')
        NOT NULL DEFAULT 'OTHER',

    occupation VARCHAR(150) NULL,
    school_or_company VARCHAR(200) NULL,

    id_type VARCHAR(50) NULL,
    id_number VARCHAR(100) NULL,

    address VARCHAR(255) NULL,

    status ENUM('ACTIVE', 'INACTIVE')
        NOT NULL DEFAULT 'ACTIVE',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_tenants_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_tenants_owner
        FOREIGN KEY (owner_id)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    -- One user account can have only one tenant profile.
    UNIQUE KEY uk_tenants_user_id (user_id),

    -- Tenant code only needs to be unique under the same owner.
    UNIQUE KEY uk_tenants_owner_code (owner_id, tenant_code),

    -- Phone only needs to be unique under the same owner.
    UNIQUE KEY uk_tenants_owner_phone (owner_id, phone),

    INDEX idx_tenants_owner_id (owner_id),

    INDEX idx_tenants_status (status)

) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;
