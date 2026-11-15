package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDateTime;

/**
 * Shared amenity assigned at the listing level (mirrors {@link RoomAmenityResponse}).
 */
public record ListingAmenityResponse(
        Long listingId,
        Long amenityId,
        String amenityName,
        String amenityCode,
        LocalDateTime createdAt
) {
}