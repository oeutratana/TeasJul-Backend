package com.example.spring_boot_project_api.dto.response;

/**
 * The current user's security settings, used by the Security settings screen.
 * Sensitive fields (hashes, tokens) are never exposed.
 */
public record UserSecurityResponse(
        String recoveryEmail,
        boolean recoveryEmailVerified,
        java.time.LocalDateTime recoveryEmailVerifiedAt,
        boolean twoFactorEnabled,
        String twoFactorMethod
) {
}
