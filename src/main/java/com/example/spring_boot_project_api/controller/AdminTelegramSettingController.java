package com.example.spring_boot_project_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.UpdateSuperAdminTelegramSettingsRequest;
import com.example.spring_boot_project_api.dto.response.SuperAdminTelegramSettingsResponse;
import com.example.spring_boot_project_api.service.SystemSettingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * SUPER_ADMIN Telegram destination management, used to notify the super admin
 * when an owner submits a property for approval.
 *
 * <p>Authorization is enforced server-side in {@link SystemSettingService}, not
 * by the UI. The bot token is write-only: it is never returned by this API.</p>
 */
@RestController
@RequestMapping("/api/admin/settings/telegram")
@RequiredArgsConstructor
@Tag(name = "Admin Telegram Settings",
        description = "SUPER_ADMIN Telegram destination for property submission notifications. "
                + "The bot token is write-only and never returned.")
public class AdminTelegramSettingController {

    private final SystemSettingService systemSettingService;

    @Operation(summary = "Get super admin Telegram settings",
            description = "Returns the enabled flag, the destination chat id, whether a bot token is stored "
                    + "and a fixed mask. The token itself is never returned. Requires the SUPER_ADMIN role.")
    @GetMapping
    public ResponseEntity<SuperAdminTelegramSettingsResponse> getTelegramSettings() {
        return ResponseEntity.ok(systemSettingService.getSuperAdminTelegramSettings());
    }

    @Operation(summary = "Update super admin Telegram settings",
            description = "Stores the Telegram destination. A blank botToken - or the mask returned by the API - "
                    + "keeps the stored token unchanged. Requires the SUPER_ADMIN role.")
    @PutMapping
    public ResponseEntity<SuperAdminTelegramSettingsResponse> updateTelegramSettings(
            @Valid @RequestBody UpdateSuperAdminTelegramSettingsRequest request) {
        return ResponseEntity.ok(systemSettingService.updateSuperAdminTelegramSettings(request));
    }
}
