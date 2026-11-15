package com.example.spring_boot_project_api.dto.response;

public record RoomTypeImageResponse(
        Long id,
        String secureUrl,
        Boolean isPrimary,
        Integer sortOrder
) {
}