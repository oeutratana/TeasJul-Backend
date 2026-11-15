-- notifications.data: Hibernate emits `cast(? as json)` for @JdbcTypeCode(SqlTypes.JSON),
-- which is invalid on MariaDB (JSON is an alias for LONGTEXT there) and caused SQL error
-- 1064 on every notification INSERT. Store the JSON payload as a plain LONGTEXT string
-- (works on both MySQL and MariaDB).
ALTER TABLE notifications MODIFY data LONGTEXT NULL;