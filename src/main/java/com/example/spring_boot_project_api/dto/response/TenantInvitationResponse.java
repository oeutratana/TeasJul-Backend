package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDateTime;

import com.example.spring_boot_project_api.model.TenantInvitationStatus;

public record TenantInvitationResponse(
        Long id,
        Long houseId,
        String houseName,
        String email,
        TenantInvitationStatus status,
        LocalDateTime expiresAt,
        LocalDateTime usedAt,
        LocalDateTime createdAt
) {
}