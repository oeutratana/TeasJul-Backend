package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDateTime;

public record PermissionResponse(
        Long id,
        String name,
        String code,
        String module,
        String description,
        LocalDateTime createdAt
) {
}
