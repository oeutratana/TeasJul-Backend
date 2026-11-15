package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Verifies a recovery email using the OTP delivered to it.
 */
public record VerifyRecoveryEmailRequest(

        @NotBlank(message = "Recovery email is required")
        @Size(max = 150, message = "Recovery email must be at most 150 characters")
        String recoveryEmail,

        @NotBlank(message = "OTP is required")
        @Pattern(regexp = "\\d{6}", message = "OTP must be a 6-digit code")
        String otp
) {
}
