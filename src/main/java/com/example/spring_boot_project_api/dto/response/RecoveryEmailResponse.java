package com.example.spring_boot_project_api.dto.response;

/**
 * Masked view of a recovery email (e.g. j***[e@gmail.com]) so the full address
 * is never returned to callers.
 */
public record RecoveryEmailResponse(
        boolean configured,
        boolean verified,
        String maskedEmail
) {
}
