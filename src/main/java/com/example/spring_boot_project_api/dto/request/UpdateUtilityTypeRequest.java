package com.example.spring_boot_project_api.dto.request;

import com.example.spring_boot_project_api.enums.UtilityTypeStatus;

import jakarta.validation.constraints.Size;

public record UpdateUtilityTypeRequest(
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        @Size(max = 50, message = "Code must not exceed 50 characters")
        String code,

        @Size(max = 30, message = "Unit must not exceed 30 characters")
        String unit,

        com.example.spring_boot_project_api.enums.UtilityBillingMethod billingMethod,

        UtilityTypeStatus status
) {
}
