package com.example.spring_boot_project_api.dto.request;

import com.example.spring_boot_project_api.model.MaintenanceRequestStatus;

import jakarta.validation.constraints.NotNull;

public record ChangeMaintenanceRequestStatusRequest(

        @NotNull(message = "Status is required")
        MaintenanceRequestStatus status
) {
}
