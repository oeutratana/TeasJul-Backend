-- SUPER_ADMIN property posting can attach a property to an owner record (a
-- non-login owner) that has no TeasJul User. In that case `houses.owner_id`
-- and `rooms.owner_id` stay NULL and ownership is carried by
-- `owner_record_id`. The NOT NULL constraints still present from before this
-- flow existed make the INSERT fail with a 500, so relax them. Hibernate
-- ddl-auto=update will not alter nullability for us.
ALTER TABLE houses
    MODIFY owner_id BIGINT UNSIGNED NULL;

ALTER TABLE rooms
    MODIFY owner_id BIGINT UNSIGNED NULL;

-- `room_utilities` composite FKs
--   (room_id, owner_id)         -> rooms (id, owner_id)
--   (utility_type_id, owner_id) -> utility_types (id, owner_id)
-- require the assignment owner to equal the room/type owner. Record-owned
-- properties have `rooms.owner_id` NULL (the owner is a non-login
-- PropertyOwner), so admin-posted utility assignments carry the issuing
-- SUPER_ADMIN as owner and must be keyed by room / utility type alone.
ALTER TABLE room_utilities
    DROP FOREIGN KEY fk_room_utilities_room;

ALTER TABLE room_utilities
    DROP FOREIGN KEY fk_room_utilities_type;

ALTER TABLE room_utilities
    ADD CONSTRAINT fk_room_utilities_room
    FOREIGN KEY (room_id) REFERENCES rooms (id)
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE room_utilities
    ADD CONSTRAINT fk_room_utilities_type
    FOREIGN KEY (utility_type_id) REFERENCES utility_types (id)
    ON DELETE RESTRICT ON UPDATE CASCADE;

-- `utility_rates` has the same owner-coupled composite FKs
--   (house_id, owner_id)         -> houses (id, owner_id)
--   (utility_type_id, owner_id)  -> utility_types (id, owner_id)
-- which reject rates for record-owned houses. Keep the simple
-- `fk_utility_rates_owner` -> users FK and key the other two by id alone.
ALTER TABLE utility_rates
    DROP FOREIGN KEY fk_utility_rates_house_owner;

ALTER TABLE utility_rates
    DROP FOREIGN KEY fk_utility_rates_type_owner;

ALTER TABLE utility_rates
    ADD CONSTRAINT fk_utility_rates_house_owner
    FOREIGN KEY (house_id) REFERENCES houses (id)
    ON UPDATE CASCADE;

ALTER TABLE utility_rates
    ADD CONSTRAINT fk_utility_rates_type_owner
    FOREIGN KEY (utility_type_id) REFERENCES utility_types (id)
    ON UPDATE CASCADE;