package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

import com.example.spring_boot_project_api.enums.NotificationType;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String message,
        Map<String, Object> data,
        LocalDateTime readAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}