package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDateTime;

import com.example.spring_boot_project_api.model.MaintenanceRequestPriority;
import com.example.spring_boot_project_api.model.MaintenanceRequestStatus;

public record MaintenanceRequestResponse(
        Long id,
        Long ownerId,
        String requestNo,
        Long houseId,
        Long roomId,
        Long tenantId,
        Long categoryId,
        String title,
        String description,
        MaintenanceRequestPriority priority,
        MaintenanceRequestStatus status,
        LocalDateTime reportedAt,
        LocalDateTime resolvedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
