package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDateTime;

public record MaintenanceAssignmentResponse(
        Long id,
        Long requestId,
        Long assignedTo,
        LocalDateTime assignedAt,
        LocalDateTime completedAt,
        String notes
) {
}
