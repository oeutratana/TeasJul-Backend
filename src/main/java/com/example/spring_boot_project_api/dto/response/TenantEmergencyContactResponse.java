package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDateTime;

public record TenantEmergencyContactResponse(
        Long id,
        Long tenantId,
        String name,
        String relationship,
        String phone,
        String address,
        Boolean isPrimary,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
