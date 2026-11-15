package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Verifies an email 2FA code using a temporary challenge token that was issued
 * only after the user supplied correct credentials.
 */
public record VerifyTwoFactorRequest(

        @NotBlank(message = "Challenge token is required")
        String challengeToken,

        @NotBlank(message = "OTP is required")
        @Pattern(regexp = "\\d{6}", message = "OTP must be a 6-digit code")
        String otp
) {
}
