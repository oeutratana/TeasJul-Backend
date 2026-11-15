package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.CreateSystemDefaultRequest;
import com.example.spring_boot_project_api.dto.request.UpdateSystemDefaultRequest;
import com.example.spring_boot_project_api.dto.request.UpdateSystemDefaultStatusRequest;
import com.example.spring_boot_project_api.dto.response.SystemDefaultResponse;
import com.example.spring_boot_project_api.enums.SystemDefaultKind;
import com.example.spring_boot_project_api.enums.SystemDefaultStatus;
import com.example.spring_boot_project_api.service.SystemDefaultService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * SUPER_ADMIN management of system default utilities &amp; amenities.
 * Authorization is enforced server-side in {@link SystemDefaultService}.
 */
@RestController
@RequestMapping("/api/admin/system-defaults")
@RequiredArgsConstructor
@Tag(name = "System Defaults Administration",
        description = "SUPER_ADMIN management of system default utilities and amenities.")
public class SystemDefaultAdminController {

    private final SystemDefaultService systemDefaultService;

    @Operation(summary = "Create a system default",
            description = "Defines a reusable utility or amenity default. "
                    + "Requires the SUPER_ADMIN role. Code and name must be "
                    + "unique per kind.")
    @PostMapping
    public ResponseEntity<SystemDefaultResponse> createDefault(
            @Valid @RequestBody CreateSystemDefaultRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(systemDefaultService.createDefault(request));
    }

    @Operation(summary = "List system defaults",
            description = "Lists all system defaults with optional kind, status "
                    + "and search filters. Requires the SUPER_ADMIN role.")
    @GetMapping
    public ResponseEntity<List<SystemDefaultResponse>> listDefaults(
            @RequestParam(required = false) SystemDefaultKind kind,
            @RequestParam(required = false) SystemDefaultStatus status,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(systemDefaultService.listDefaults(kind, status, search));
    }

    @Operation(summary = "Update a system default",
            description = "Updates the system default with the given id. Only "
                    + "provided fields are updated. Requires the SUPER_ADMIN role.")
    @PutMapping("/{id}")
    public ResponseEntity<SystemDefaultResponse> updateDefault(@PathVariable Long id,
            @Valid @RequestBody UpdateSystemDefaultRequest request) {
        return ResponseEntity.ok(systemDefaultService.updateDefault(id, request));
    }

    @Operation(summary = "Activate/deactivate a system default",
            description = "Marks the system default with the given id as ACTIVE "
                    + "or INACTIVE. Requires the SUPER_ADMIN role. Deactivating "
                    + "never deletes existing owner copies or their data.")
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateDefaultStatus(@PathVariable Long id,
            @Valid @RequestBody UpdateSystemDefaultStatusRequest request) {
        systemDefaultService.updateDefaultStatus(id, request.status());
        return ResponseEntity.noContent().build();
    }
}