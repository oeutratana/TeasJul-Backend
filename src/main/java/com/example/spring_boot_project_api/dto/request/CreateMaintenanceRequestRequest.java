package com.example.spring_boot_project_api.dto.request;

import com.example.spring_boot_project_api.model.MaintenanceRequestPriority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateMaintenanceRequestRequest(

        @NotNull(message = "House id is required")
        @Positive(message = "House id must be positive")
        Long houseId,

        @NotNull(message = "Room id is required")
        @Positive(message = "Room id must be positive")
        Long roomId,

        @Positive(message = "Tenant id must be positive")
        Long tenantId,

        @Positive(message = "Category id must be positive")
        Long categoryId,

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must not exceed 200 characters")
        String title,

        String description,

        MaintenanceRequestPriority priority
) {
}
