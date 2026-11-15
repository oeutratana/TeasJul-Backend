package com.example.spring_boot_project_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.UpdateOwnerSettingsRequest;
import com.example.spring_boot_project_api.dto.response.OwnerSettingsResponse;
import com.example.spring_boot_project_api.service.OwnerSettingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@Tag(name = "Owner Settings", description = "Owner business and application settings")
public class OwnerSettingController {

    private final OwnerSettingService ownerSettingService;

    @Operation(summary = "Get owner settings")
    @GetMapping
    public ResponseEntity<OwnerSettingsResponse> getSettings() {
        return ResponseEntity.ok(ownerSettingService.getSettings());
    }

    @Operation(summary = "Update owner settings")
    @PutMapping
    public ResponseEntity<OwnerSettingsResponse> updateSettings(
            @Valid @RequestBody UpdateOwnerSettingsRequest request) {
        return ResponseEntity.ok(ownerSettingService.updateSettings(request));
    }
}
