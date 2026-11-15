package com.example.spring_boot_project_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.UpdateSystemNotificationSettingsRequest;
import com.example.spring_boot_project_api.dto.response.SystemNotificationSettingsResponse;
import com.example.spring_boot_project_api.service.SystemSettingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * SUPER_ADMIN system notification settings management.
 * Authorization is enforced server-side in {@link SystemSettingService}.
 */
@RestController
@RequestMapping("/api/admin/settings/notifications")
@RequiredArgsConstructor
@Tag(name = "Admin Notification Settings",
        description = "SUPER_ADMIN configuration of system-wide notification preferences.")
public class AdminNotificationSettingController {

    private final SystemSettingService systemSettingService;

    @Operation(summary = "Get system notification settings",
            description = "Retrieves global system notification toggles and event type configurations. "
                    + "Requires the SUPER_ADMIN role.")
    @GetMapping
    public ResponseEntity<SystemNotificationSettingsResponse> getNotificationSettings() {
        return ResponseEntity.ok(systemSettingService.getNotificationSettings());
    }

    @Operation(summary = "Update system notification settings",
            description = "Updates global system notification toggles and event type configurations. "
                    + "Requires the SUPER_ADMIN role.")
    @PutMapping
    public ResponseEntity<SystemNotificationSettingsResponse> updateNotificationSettings(
            @Valid @RequestBody UpdateSystemNotificationSettingsRequest request) {
        return ResponseEntity.ok(systemSettingService.updateNotificationSettings(request));
    }
}
