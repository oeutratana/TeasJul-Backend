package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDateTime;

public record MaintenanceCommentResponse(
        Long id,
        Long requestId,
        Long userId,
        String comment,
        LocalDateTime createdAt
) {
}
