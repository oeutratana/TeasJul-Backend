package com.example.spring_boot_project_api.dto.response;

/**
 * Minimal public representation of a room type used to populate filters on the
 * public room listing. Only the fields needed for browsing are exposed.
 */
public record PublicRoomTypeResponse(
        Long id,
        String name
) {
}