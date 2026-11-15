package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDateTime;

/**
 * Public validation result for the invite page. Reason values:
 * VALID, INVALID_TOKEN, INVITATION_EXPIRED, INVITATION_ALREADY_USED,
 * INVITATION_REVOKED. No token material is ever exposed.
 */
public record InvitationValidationResponse(
        boolean valid,
        String reason,
        String email,
        String houseName,
        LocalDateTime expiresAt
) {
}