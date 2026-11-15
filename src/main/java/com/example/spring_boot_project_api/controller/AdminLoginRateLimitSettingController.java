package com.example.spring_boot_project_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.UpdateLoginRateLimitSettingsRequest;
import com.example.spring_boot_project_api.dto.response.LoginRateLimitSettingsResponse;
import com.example.spring_boot_project_api.service.SystemSettingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * SUPER_ADMIN login rate-limiting settings management.
 * Authorization is enforced server-side in {@link SystemSettingService}.
 */
@RestController
@RequestMapping("/api/admin/settings/security/login-rate-limit")
@RequiredArgsConstructor
@Tag(name = "Admin Login Rate Limit Settings",
        description = "SUPER_ADMIN configuration of the login rate limiter (attempts per account and lockout window).")
public class AdminLoginRateLimitSettingController {

    private final SystemSettingService systemSettingService;

    @Operation(summary = "Get login rate limit settings",
            description = "Retrieves the login rate limiter toggles, maximum failed attempts and lockout duration. "
                    + "Requires the SUPER_ADMIN role.")
    @GetMapping
    public ResponseEntity<LoginRateLimitSettingsResponse> getLoginRateLimitSettings() {
        return ResponseEntity.ok(systemSettingService.getLoginRateLimitSettings());
    }

    @Operation(summary = "Update login rate limit settings",
            description = "Updates the login rate limiter toggle, maximum failed attempts and lockout duration. "
                    + "The existing login limiter picks the values up immediately. Requires the SUPER_ADMIN role.")
    @PutMapping
    public ResponseEntity<LoginRateLimitSettingsResponse> updateLoginRateLimitSettings(
            @Valid @RequestBody UpdateLoginRateLimitSettingsRequest request) {
        return ResponseEntity.ok(systemSettingService.updateLoginRateLimitSettings(request));
    }
}
