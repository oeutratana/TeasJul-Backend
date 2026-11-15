package com.example.spring_boot_project_api.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

/**
 * Assigns one or more amenities to many rooms in a single operation.
 *
 * <p>(room, amenity) pairs that are already assigned are reported back in the response
 * summary instead of being duplicated.
 */
public record BulkRoomAmenityRequest(
        @NotEmpty(message = "At least one room is required")
        List<Long> roomIds,

        @NotEmpty(message = "At least one amenity is required")
        List<Long> amenityIds
) {
}