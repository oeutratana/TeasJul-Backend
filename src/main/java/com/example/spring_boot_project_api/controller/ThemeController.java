package com.example.spring_boot_project_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.UpdateThemeRequest;
import com.example.spring_boot_project_api.dto.response.ThemeSettingsResponse;
import com.example.spring_boot_project_api.service.OwnerSettingService;
import com.example.spring_boot_project_api.util.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/settings/theme")
@RequiredArgsConstructor
@Tag(name = "Theme Settings", description = "System theme (appearance) shared by the owner and the super admin. Modifications are OWNER or SUPER_ADMIN only.")
public class ThemeController {

    private final OwnerSettingService ownerSettingService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "Get the organization theme (available to any authenticated user)")
    @GetMapping
    public ResponseEntity<ThemeSettingsResponse> getTheme() {
        return ResponseEntity.ok(ownerSettingService.getTheme());
    }

    @Operation(summary = "Update the system theme (OWNER or SUPER_ADMIN)")
    @PutMapping
    public ResponseEntity<ThemeSettingsResponse> updateTheme(
            @Valid @RequestBody UpdateThemeRequest request) {
        requireThemeEditor();
        return ResponseEntity.ok(ownerSettingService.updateTheme(request));
    }

    @Operation(summary = "Reset the system theme to defaults (OWNER or SUPER_ADMIN)")
    @PostMapping("/reset")
    public ResponseEntity<ThemeSettingsResponse> resetTheme() {
        requireThemeEditor();
        return ResponseEntity.ok(ownerSettingService.resetTheme());
    }

    /**
     * The system theme is a single shared value, so both the owner and the super
     * admin may edit it. MANAGER and TENANT stay read-only.
     */
    private void requireThemeEditor() {
        if (!securityUtils.hasRole(SecurityUtils.ROLE_OWNER)
                && !securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)) {
            throw new AccessDeniedException("Only owners and super admins can modify the theme");
        }
    }
}