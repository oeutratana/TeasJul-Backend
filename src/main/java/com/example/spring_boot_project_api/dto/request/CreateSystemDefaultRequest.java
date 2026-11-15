package com.example.spring_boot_project_api.dto.request;

import com.example.spring_boot_project_api.enums.SystemDefaultKind;
import com.example.spring_boot_project_api.enums.UtilityBillingMethod;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSystemDefaultRequest(
        @NotNull(message = "Kind is required")
        SystemDefaultKind kind,

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        @NotBlank(message = "Code is required")
        @Size(max = 50, message = "Code must not exceed 50 characters")
        String code,

        @Size(max = 30, message = "Unit must not exceed 30 characters")
        String unit,

        UtilityBillingMethod billingMethod,

        String description,

        @Size(max = 100, message = "Icon path/name must not exceed 100 characters")
        String icon
) {
}