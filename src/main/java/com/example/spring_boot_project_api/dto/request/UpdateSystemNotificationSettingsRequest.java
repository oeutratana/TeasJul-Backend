package com.example.spring_boot_project_api.dto.request;

public record UpdateSystemNotificationSettingsRequest(
        Boolean systemNotificationsEnabled,
        Boolean inAppNotificationsEnabled,
        Boolean telegramNotificationsEnabled,
        Boolean propertySubmittedEnabled,
        Boolean invoiceCreatedEnabled,
        Boolean paymentCreatedEnabled,
        Boolean paymentSuccessEnabled,
        Boolean paymentFailedEnabled,
        Boolean maintenanceRequestEnabled,
        Boolean maintenanceStatusEnabled,
        Boolean contractExpiringEnabled,
        Boolean roomAssignedEnabled
) {
}
