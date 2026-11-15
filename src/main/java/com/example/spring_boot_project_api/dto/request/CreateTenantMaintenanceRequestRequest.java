package com.example.spring_boot_project_api.dto.request;

import com.example.spring_boot_project_api.model.MaintenanceRequestPriority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTenantMaintenanceRequestRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must not exceed 200 characters")
        String title,

        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description,

        MaintenanceRequestPriority priority
) {
}