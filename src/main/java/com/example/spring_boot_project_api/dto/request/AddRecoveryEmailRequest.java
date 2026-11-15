package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to set/update the recovery email on the current user's account and
 * send an OTP to it. The recovery email is never trusted until verified.
 */
public record AddRecoveryEmailRequest(

        @NotBlank(message = "Recovery email is required")
        @Email(message = "Recovery email must be a valid email address")
        @Size(max = 150, message = "Recovery email must be at most 150 characters")
        String recoveryEmail
) {
}
