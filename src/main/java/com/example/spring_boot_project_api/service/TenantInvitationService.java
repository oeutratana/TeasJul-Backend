package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.ConsumeTenantInvitationRequest;
import com.example.spring_boot_project_api.dto.request.CreateTenantInvitationRequest;
import com.example.spring_boot_project_api.dto.response.InvitationConsumeResponse;
import com.example.spring_boot_project_api.dto.response.InvitationValidationResponse;
import com.example.spring_boot_project_api.dto.response.TenantInvitationCreateResponse;
import com.example.spring_boot_project_api.dto.response.TenantInvitationResponse;

public interface TenantInvitationService {

    TenantInvitationCreateResponse createInvitation(CreateTenantInvitationRequest request);

    List<TenantInvitationResponse> getMyInvitations();

    InvitationValidationResponse validateInvitation(String token);

    InvitationConsumeResponse consumeInvitation(ConsumeTenantInvitationRequest request);

    TenantInvitationResponse revokeInvitation(Long id);
}