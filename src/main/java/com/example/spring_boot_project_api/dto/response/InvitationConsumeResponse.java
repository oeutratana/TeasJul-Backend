package com.example.spring_boot_project_api.dto.response;

public record InvitationConsumeResponse(
        boolean success,
        Long tenantId,
        String username,
        String email,
        String message
) {
}