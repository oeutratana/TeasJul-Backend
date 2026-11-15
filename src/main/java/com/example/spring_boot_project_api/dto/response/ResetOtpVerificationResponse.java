package com.example.spring_boot_project_api.dto.response;

/**
 * Returned after a successful password-reset OTP verification. Carries a
 * short-lived reset token plus masking info so the frontend can display a
 * partially hidden recovery email without ever seeing the full address.
 */
public record ResetOtpVerificationResponse(
        String resetToken,
        long expiresIn,
        String maskedRecoveryEmail
) {
}
