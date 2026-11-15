package com.example.spring_boot_project_api.util;

import com.example.spring_boot_project_api.enums.PropertyType;

/**
 * Validates that a listing's room-specific fields (room number, room type,
 * capacity, floor) match the selected property type. STRUCTURAL types
 * (HOUSE / VILLA / OTHER) do not have physical room numbers, APARTMENT /
 * CONDO units use the unit number as the "room number" plus a floor, and
 * ROOM listings keep full room identity.
 */
public final class PropertyTypeRules {

    private PropertyTypeRules() {
    }

    public static boolean isRoomLike(PropertyType type) {
        return type == null || type == PropertyType.ROOM;
    }

    public static boolean isUnitLike(PropertyType type) {
        return type == PropertyType.APARTMENT || type == PropertyType.CONDO;
    }

    public static boolean isStructureLike(PropertyType type) {
        return type == PropertyType.HOUSE || type == PropertyType.VILLA
                || type == PropertyType.OTHER;
    }

    /**
     * Validate that the supplied room-identity fields are consistent with the
     * given property type. Throws {@link IllegalArgumentException} (mapped to
     * HTTP 400 by GlobalExceptionHandler) when required fields are missing.
     */
    public static void validate(PropertyType type, Long roomTypeId, String roomNumber,
            Integer floorNumber, Integer capacity) {
        if (isRoomLike(type)) {
            if (roomTypeId == null) {
                throw new IllegalArgumentException(
                        "Room type is required for ROOM listings");
            }
            if (roomNumber == null || roomNumber.isBlank()) {
                throw new IllegalArgumentException(
                        "Room number is required for ROOM listings");
            }
            if (capacity == null || capacity < 1) {
                throw new IllegalArgumentException(
                        "Capacity must be greater than 0 for ROOM listings");
            }
        } else if (isUnitLike(type)) {
            if (roomNumber == null || roomNumber.isBlank()) {
                throw new IllegalArgumentException(
                        "Unit number is required for APARTMENT / CONDO listings");
            }
            if (floorNumber == null) {
                throw new IllegalArgumentException(
                        "Floor is required for APARTMENT / CONDO listings");
            }
        }
        // structure types → no room-identity fields required
    }
}
