package com.example.spring_boot_project_api.dto.request;

import com.example.spring_boot_project_api.model.MaintenanceRequestPriority;

import jakarta.validation.constraints.Size;

public record UpdateMaintenanceRequestRequest(

        @Size(max = 200, message = "Title must not exceed 200 characters")
        String title,

        String description,

        MaintenanceRequestPriority priority
) {
}
