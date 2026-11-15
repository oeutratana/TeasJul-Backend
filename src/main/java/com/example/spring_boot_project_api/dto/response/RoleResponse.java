package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDateTime;

public record RoleResponse(
        Long id,
        String name,
        String code,
        String description,
        Boolean isSystemRole,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
