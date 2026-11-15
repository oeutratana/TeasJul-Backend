package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDateTime;

public record SystemNotificationSettingsResponse(
        boolean systemNotificationsEnabled,
        boolean inAppNotificationsEnabled,
        boolean telegramNotificationsEnabled,
        boolean propertySubmittedEnabled,
        boolean invoiceCreatedEnabled,
        boolean paymentCreatedEnabled,
        boolean paymentSuccessEnabled,
        boolean paymentFailedEnabled,
        boolean maintenanceRequestEnabled,
        boolean maintenanceStatusEnabled,
        boolean contractExpiringEnabled,
        boolean roomAssignedEnabled,
        LocalDateTime updatedAt
) {
}
