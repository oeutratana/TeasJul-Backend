package com.example.spring_boot_project_api.dto.response;

/**
 * Gallery image of a grouped listing (mirrors {@link HouseImageResponse}).
 */
public record ListingImageResponse(
        Long id,
        String secureUrl,
        Boolean isPrimary,
        Integer sortOrder
) {
}