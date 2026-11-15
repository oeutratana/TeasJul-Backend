package com.example.spring_boot_project_api.dto.request;

import com.example.spring_boot_project_api.enums.SystemDefaultStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateSystemDefaultStatusRequest(
        @NotNull(message = "Status is required")
        SystemDefaultStatus status
) {
}