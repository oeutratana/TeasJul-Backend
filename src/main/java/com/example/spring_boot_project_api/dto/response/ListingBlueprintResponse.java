package com.example.spring_boot_project_api.dto.response;

/**
 * Floor plan / blueprint of a grouped listing (single Cloudinary asset).
 */
public record ListingBlueprintResponse(
        Long listingId,
        String blueprintPublicId,
        String blueprintSecureUrl
) {
}