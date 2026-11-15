package com.example.spring_boot_project_api.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

import com.example.spring_boot_project_api.dto.request.CreateOwnerRequest;
import com.example.spring_boot_project_api.dto.request.UpdateOwnerStatusRequest;
import com.example.spring_boot_project_api.dto.request.UpdateUserRequest;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.UserResponse;
import com.example.spring_boot_project_api.model.UserStatus;
import com.example.spring_boot_project_api.service.OwnerAdminService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * SUPER_ADMIN management of OWNER accounts. Authorization is enforced
 * server-side in {@link OwnerAdminService}.
 */
@RestController
@RequestMapping("/api/admin/owners")
@RequiredArgsConstructor
@Tag(name = "Owner Administration",
        description = "SUPER_ADMIN management of OWNER accounts.")
public class OwnerAdminController {

    private final OwnerAdminService ownerAdminService;

    @Operation(summary = "Create an OWNER account",
            description = "Creates a user with the existing OWNER role. The request has no role "
                    + "field; role escalation is rejected. The password is never returned.")
    @PostMapping
    public ResponseEntity<UserResponse> createOwner(@Valid @RequestBody CreateOwnerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ownerAdminService.createOwner(request));
    }

    @Operation(summary = "List OWNER accounts",
            description = "Paginated, server-side search and status filter limited to users holding "
                    + "the OWNER role. Returns only safe fields.")
    @GetMapping
    public ResponseEntity<PageResponse<UserResponse>> listOwners(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserStatus status,
            @PageableDefault(size = 10, sort = "firstName", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(ownerAdminService.listOwners(search, status, pageable));
    }

    @Operation(summary = "Get an OWNER account")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getOwner(@PathVariable Long id) {
        return ResponseEntity.ok(ownerAdminService.getOwner(id));
    }

    @Operation(summary = "Update basic OWNER profile information",
            description = "Only basic profile fields are updated. Role, ownership scope, username, "
                    + "email and password can never be changed through this endpoint.")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateOwner(@PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ownerAdminService.updateOwner(id, request));
    }

    @Operation(summary = "Activate/deactivate an OWNER account",
            description = "Reuses the existing user status mechanism (ACTIVE/INACTIVE). Deactivating "
                    + "an owner keeps all houses/rooms/data intact.")
    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponse> updateOwnerStatus(@PathVariable Long id,
            @Valid @RequestBody UpdateOwnerStatusRequest request) {
        return ResponseEntity.ok(ownerAdminService.updateOwnerStatus(id, request.status()));
    }
}