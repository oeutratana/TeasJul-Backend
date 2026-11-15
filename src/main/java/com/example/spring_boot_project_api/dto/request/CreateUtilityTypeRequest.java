package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUtilityTypeRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        @NotBlank(message = "Code is required")
        @Size(max = 50, message = "Code must not exceed 50 characters")
        String code,

        @NotBlank(message = "Unit is required")
        @Size(max = 30, message = "Unit must not exceed 30 characters")
        String unit,

        com.example.spring_boot_project_api.enums.UtilityBillingMethod billingMethod
) {
}
