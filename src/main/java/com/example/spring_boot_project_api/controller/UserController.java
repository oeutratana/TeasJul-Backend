package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.UpdateUserRequest;
import com.example.spring_boot_project_api.dto.response.HouseResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.RoleResponse;
import com.example.spring_boot_project_api.dto.response.UserResponse;
import com.example.spring_boot_project_api.dto.response.UserSummaryResponse;
import com.example.spring_boot_project_api.dto.response.UserTenantCandidateResponse;
import com.example.spring_boot_project_api.model.UserStatus;
import com.example.spring_boot_project_api.service.UserHouseAccessService;
import com.example.spring_boot_project_api.service.UserRoleService;
import com.example.spring_boot_project_api.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

    private final UserService userService;
    private final UserHouseAccessService userHouseAccessService;
    private final UserRoleService userRoleService;

    @Operation(summary = "Search users available for tenant linking",
            description = "Paginated, server-side search of users that do not yet have a tenant profile. "
                    + "Searches by name, full name, email, phone or username. Returns only a lightweight "
                    + "candidate DTO (no sensitive fields). Page size is capped server side.")
    @GetMapping("/available-for-tenant")
    public ResponseEntity<PageResponse<UserTenantCandidateResponse>> searchAvailableForTenant(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 10, sort = "firstName", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(userService.searchAvailableForTenant(q, pageable));
    }

    @Operation(summary = "List all users",
            description = "Paginated, server-side search and status filter for all users in the system. "
                    + "Only accessible to owners. Page size is capped server side.")
    @GetMapping
    public ResponseEntity<PageResponse<UserResponse>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserStatus status,
            @PageableDefault(size = 10, sort = "firstName", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(search, status, pageable));
    }

    @Operation(summary = "User summary",
            description = "Lightweight aggregate (total count + per-status counts) for admin stat cards.")
    @GetMapping("/summary")
    public ResponseEntity<UserSummaryResponse> getUsersSummary() {
        return ResponseEntity.ok(userService.getUsersSummary());
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    @GetMapping("/me/roles")
    public ResponseEntity<List<RoleResponse>> getCurrentUserRoles() {
        return ResponseEntity.ok(userRoleService.getCurrentUserRoles());
    }

    @GetMapping("/me/houses")
    public ResponseEntity<List<HouseResponse>> getMyAccessibleHouses() {
        return ResponseEntity.ok(userHouseAccessService.getUserHouses());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
