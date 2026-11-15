package com.example.spring_boot_project_api.dto.request;

import com.example.spring_boot_project_api.model.MaintenanceCategoryStatus;

import jakarta.validation.constraints.Size;

public record UpdateMaintenanceCategoryRequest(

        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        @Size(max = 50, message = "Code must not exceed 50 characters")
        String code,

        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description,

        MaintenanceCategoryStatus status
) {
}
