package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDateTime;

import com.example.spring_boot_project_api.enums.UtilityTypeStatus;

public record UtilityTypeResponse(
        Long id,
        Long ownerId,
        String name,
        String code,
        String unit,
        com.example.spring_boot_project_api.enums.UtilityBillingMethod billingMethod,
        UtilityTypeStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
