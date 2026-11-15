package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Initiates the password-reset flow for the given system/login email.
 * The response is deliberately generic to prevent account enumeration.
 */
public record ForgotPasswordRequest(

        @NotBlank(message = "Email is required")
        @Size(max = 150, message = "Email must be at most 150 characters")
        String email
) {
}
