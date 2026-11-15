package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDateTime;

import com.example.spring_boot_project_api.enums.SystemDefaultKind;
import com.example.spring_boot_project_api.enums.SystemDefaultStatus;
import com.example.spring_boot_project_api.enums.UtilityBillingMethod;

public record SystemDefaultResponse(
        Long id,
        SystemDefaultKind kind,
        String name,
        String code,
        String unit,
        UtilityBillingMethod billingMethod,
        String description,
        String icon,
        SystemDefaultStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}