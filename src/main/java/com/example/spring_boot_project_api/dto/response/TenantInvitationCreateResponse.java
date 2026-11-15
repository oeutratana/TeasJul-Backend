package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDateTime;

import com.example.spring_boot_project_api.model.TenantInvitationStatus;

/**
 * Returned only by the create endpoint. The raw one-time token is present here
 * and nowhere else — it is not persisted by the backend, so it cannot be
 * retrieved later from the list endpoint.
 */
public record TenantInvitationCreateResponse(
        Long id,
        String token,
        String invitationPath,
        String email,
        Long houseId,
        String houseName,
        LocalDateTime expiresAt,
        TenantInvitationStatus status
) {
}