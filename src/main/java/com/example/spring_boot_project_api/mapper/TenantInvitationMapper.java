package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.TenantInvitationCreateResponse;
import com.example.spring_boot_project_api.dto.response.TenantInvitationResponse;
import com.example.spring_boot_project_api.model.TenantInvitation;

@Component
public class TenantInvitationMapper {

    public TenantInvitationCreateResponse toCreateResponse(TenantInvitation invitation,
            String rawToken, String invitationPath) {
        return new TenantInvitationCreateResponse(
                invitation.getId(),
                rawToken,
                invitationPath,
                invitation.getEmail(),
                invitation.getHouse().getId(),
                invitation.getHouse().getName(),
                invitation.getExpiresAt(),
                invitation.getStatus());
    }

    public TenantInvitationResponse toResponse(TenantInvitation invitation) {
        return new TenantInvitationResponse(
                invitation.getId(),
                invitation.getHouse().getId(),
                invitation.getHouse().getName(),
                invitation.getEmail(),
                invitation.getStatus(),
                invitation.getExpiresAt(),
                invitation.getUsedAt(),
                invitation.getCreatedAt());
    }
}