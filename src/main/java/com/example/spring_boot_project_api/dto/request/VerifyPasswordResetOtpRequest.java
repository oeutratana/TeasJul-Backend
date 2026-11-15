package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Verifies the password-reset OTP. On success the backend issues a short-lived,
 * single-use reset token that must be used to actually change the password.
 */
public record VerifyPasswordResetOtpRequest(

        @NotBlank(message = "Email is required")
        @Size(max = 150, message = "Email must be at most 150 characters")
        String email,

        @NotBlank(message = "OTP is required")
        @Pattern(regexp = "\\d{6}", message = "OTP must be a 6-digit code")
        String otp
) {
}
