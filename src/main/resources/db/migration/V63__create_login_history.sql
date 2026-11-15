-- Login history: one immutable row per sign-in attempt, so a user (and a
-- SUPER_ADMIN) can review where and how their account was accessed.
--
-- user_id is NULLABLE on purpose. A failed attempt against an identifier that
-- does not exist cannot be attributed to any account, but it is exactly the
-- signal that makes failed-login history useful (credential stuffing / username
-- enumeration), so those attempts are still recorded. Successful attempts always
-- carry a user_id.
--
-- No secret is ever stored here: no password, OTP code, reset/challenge token or
-- JWT. Only the outcome, the authentication path, the client IP and the raw
-- User-Agent header.

CREATE TABLE login_history (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT UNSIGNED NULL,
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    outcome VARCHAR(10) NOT NULL,
    ip VARCHAR(45) NULL,
    user_agent VARCHAR(255) NULL,
    method VARCHAR(20) NOT NULL,

    CONSTRAINT fk_login_history_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    INDEX idx_login_history_user_occurred_at (user_id, occurred_at DESC),
    INDEX idx_login_history_outcome_occurred_at (outcome, occurred_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;
