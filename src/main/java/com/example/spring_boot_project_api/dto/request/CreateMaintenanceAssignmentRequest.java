package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateMaintenanceAssignmentRequest(

        @NotNull(message = "Assigned to user id is required")
        @Positive(message = "Assigned to user id must be positive")
        Long assignedTo,

        String notes
) {
}
