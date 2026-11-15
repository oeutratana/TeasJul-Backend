package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.ConsumeTenantInvitationRequest;
import com.example.spring_boot_project_api.dto.request.CreateTenantInvitationRequest;
import com.example.spring_boot_project_api.dto.response.InvitationConsumeResponse;
import com.example.spring_boot_project_api.dto.response.InvitationValidationResponse;
import com.example.spring_boot_project_api.dto.response.TenantInvitationCreateResponse;
import com.example.spring_boot_project_api.dto.response.TenantInvitationResponse;
import com.example.spring_boot_project_api.service.TenantInvitationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/tenant-invitations")
@RequiredArgsConstructor
public class TenantInvitationController {

    private final TenantInvitationService tenantInvitationService;

    @PostMapping
    public ResponseEntity<TenantInvitationCreateResponse> create(
            @Valid @RequestBody CreateTenantInvitationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tenantInvitationService.createInvitation(request));
    }

    @GetMapping
    public List<TenantInvitationResponse> getMyInvitations() {
        return tenantInvitationService.getMyInvitations();
    }

    @GetMapping("/validate")
    public InvitationValidationResponse validate(@RequestParam("token") String token) {
        return tenantInvitationService.validateInvitation(token);
    }

    @PostMapping("/consume")
    public InvitationConsumeResponse consume(
            @Valid @RequestBody ConsumeTenantInvitationRequest request) {
        return tenantInvitationService.consumeInvitation(request);
    }

    @DeleteMapping("/{id}")
    public TenantInvitationResponse revoke(@PathVariable Long id) {
        return tenantInvitationService.revokeInvitation(id);
    }
}