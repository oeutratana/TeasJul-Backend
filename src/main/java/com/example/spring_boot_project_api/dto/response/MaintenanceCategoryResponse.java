package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDateTime;

import com.example.spring_boot_project_api.model.MaintenanceCategoryStatus;

public record MaintenanceCategoryResponse(
        Long id,
        Long ownerId,
        String name,
        String code,
        String description,
        MaintenanceCategoryStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
