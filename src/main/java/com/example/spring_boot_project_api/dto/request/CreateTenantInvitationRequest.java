package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTenantInvitationRequest(

        @NotNull(message = "House is required")
        Long houseId,

        @Email(message = "Email must be a valid email address")
        @Size(max = 150)
        String email
) {
}