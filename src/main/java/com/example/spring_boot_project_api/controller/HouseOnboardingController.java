package com.example.spring_boot_project_api.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.spring_boot_project_api.dto.request.HouseImportRequest;
import com.example.spring_boot_project_api.dto.request.HouseManualSetupRequest;
import com.example.spring_boot_project_api.dto.request.HouseManualSetupPreviewRequest;
import com.example.spring_boot_project_api.dto.response.HouseImportPreviewResponse;
import com.example.spring_boot_project_api.dto.response.HouseImportResult;
import com.example.spring_boot_project_api.dto.response.TenantResponse;
import com.example.spring_boot_project_api.service.HouseOnboardingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * One-time onboarding for an existing house that already has rental data.
 * House ownership/manager assignment is resolved server-side from the authenticated user —
 * never from a client-supplied {@code ownerId}.
 */
@RestController
@RequestMapping("/api/houses/{houseId}/onboarding")
@RequiredArgsConstructor
@Tag(name = "House Onboarding", description = "Existing-house data import (owner or assigned manager only)")
public class HouseOnboardingController {

    private final HouseOnboardingService houseOnboardingService;

    @Operation(summary = "Parse and validate an uploaded CSV/Excel file",
            description = "Returns a row-level preview with errors/warnings. Nothing is written.")
    @PostMapping("/preview")
    public ResponseEntity<HouseImportPreviewResponse> preview(
            @PathVariable Long houseId,
            @RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        return ResponseEntity.ok(houseOnboardingService.previewImport(
                houseId, file.getOriginalFilename(), file.getBytes()));
    }

    @Operation(summary = "Confirm and transactionally import existing rental data",
            description = "Re-validates every row server-side before writing. Single transaction; "
                    + "on any error nothing is persisted.")
    @PostMapping("/import")
    public ResponseEntity<HouseImportResult> importExisting(
            @PathVariable Long houseId,
            @Valid @RequestBody HouseImportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(houseOnboardingService.importExisting(houseId, request));
    }

    @Operation(summary = "List the house owner's tenants (for manual setup UX)")
    @GetMapping("/tenants")
    public ResponseEntity<List<TenantResponse>> getHouseOwnerTenants(@PathVariable Long houseId) {
        return ResponseEntity.ok(houseOnboardingService.getHouseOwnerTenants(houseId));
    }

    @Operation(summary = "Preview manually entered existing rental data",
            description = "Validates manually entered rows and returns row-level preview with errors/warnings. Nothing is written.")
    @PostMapping("/manual/preview")
    public ResponseEntity<HouseImportPreviewResponse> previewManualSetup(
            @PathVariable Long houseId,
            @Valid @RequestBody HouseManualSetupPreviewRequest request) {
        return ResponseEntity.ok(houseOnboardingService.previewManualSetup(houseId, request));
    }

    @Operation(summary = "Confirm and transactionally save manually entered existing rental data",
            description = "Re-validates every row server-side before writing. Single transaction; "
                    + "on any error nothing is persisted.")
    @PostMapping("/manual")
    public ResponseEntity<HouseImportResult> manualSetup(
            @PathVariable Long houseId,
            @Valid @RequestBody HouseManualSetupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(houseOnboardingService.manualSetup(houseId, request));
    }
}