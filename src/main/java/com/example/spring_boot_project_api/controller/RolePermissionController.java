package com.example.spring_boot_project_api.controller;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.ReplaceRolePermissionsRequest;
import com.example.spring_boot_project_api.dto.response.PermissionResponse;
import com.example.spring_boot_project_api.service.RolePermissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@Tag(name = "Role Permissions",
        description = "Assigns and manages the permissions of a role (many-to-many).")
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;

    @Operation(summary = "List permissions assigned to a role")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Permissions of the role"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "404", description = "Role not found")
    })
    @GetMapping("/{roleId}/permissions")
    public ResponseEntity<List<PermissionResponse>> getRolePermissions(@PathVariable Long roleId) {
        return ResponseEntity.ok(rolePermissionService.getRolePermissions(roleId));
    }

    @Operation(summary = "Assign a permission to a role",
            description = "Both the role and the permission must exist, and the permission must not already be assigned to the role.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Permission assigned"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "404", description = "Role or permission not found"),
            @ApiResponse(responseCode = "409", description = "Permission already assigned to the role")
    })
    @PostMapping("/{roleId}/permissions/{permissionId}")
    public ResponseEntity<PermissionResponse> assignPermissionToRole(@PathVariable Long roleId,
            @PathVariable Long permissionId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(rolePermissionService.assignPermissionToRole(roleId, permissionId));
    }

    @Operation(summary = "Remove a permission from a role",
            description = "Only removes the assignment; neither the role nor the permission is deleted.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Permission removed from the role"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "404", description = "Role not found or assignment not found")
    })
    @DeleteMapping("/{roleId}/permissions/{permissionId}")
    public ResponseEntity<Void> removePermissionFromRole(@PathVariable Long roleId,
            @PathVariable Long permissionId) {
        rolePermissionService.removePermissionFromRole(roleId, permissionId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Replace all permissions of a role",
            description = "Replaces the full set of permissions assigned to the role with the given list. "
                    + "Every permission id must exist; duplicates in the list are ignored.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated permissions of the role"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "404", description = "Role or one of the permissions not found")
    })
    @PutMapping("/{roleId}/permissions")
    public ResponseEntity<List<PermissionResponse>> replaceRolePermissions(
            @PathVariable Long roleId,
            @Valid @RequestBody ReplaceRolePermissionsRequest request) {
        return ResponseEntity.ok(rolePermissionService.replaceRolePermissions(roleId, request));
    }
}
