package com.example.spring_boot_project_api.dto.request;

import com.example.spring_boot_project_api.model.UserStatus;

import jakarta.validation.constraints.NotNull;

/**
 * Request for activating/deactivating an OWNER account. Reuses the existing
 * {@link UserStatus} mechanism; only {@code ACTIVE} and {@code INACTIVE} are
 * accepted by the service.
 */
public record UpdateOwnerStatusRequest(

        @NotNull(message = "Status is required")
        UserStatus status
) {
}