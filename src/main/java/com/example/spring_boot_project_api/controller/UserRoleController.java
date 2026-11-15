package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.response.RoleResponse;
import com.example.spring_boot_project_api.service.UserRoleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Roles",
        description = "Assigns and manages the roles of a user (many-to-many).")
public class UserRoleController {

    private final UserRoleService userRoleService;

    @Operation(summary = "List roles assigned to a user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Roles of the user"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{userId}/roles")
    public ResponseEntity<List<RoleResponse>> getUserRoles(@PathVariable Long userId) {
        return ResponseEntity.ok(userRoleService.getUserRoles(userId));
    }

    @Operation(summary = "Assign a role to a user",
            description = "Both the user and the role must exist, and the role must not already be assigned to the user.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Role assigned"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "404", description = "User or role not found"),
            @ApiResponse(responseCode = "409", description = "Role already assigned to the user")
    })
    @PostMapping("/{userId}/roles/{roleId}")
    public ResponseEntity<RoleResponse> assignRoleToUser(@PathVariable Long userId,
            @PathVariable Long roleId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userRoleService.assignRoleToUser(userId, roleId));
    }

    @Operation(summary = "Remove a role from a user",
            description = "Only removes the assignment; neither the user nor the role is deleted.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Role removed from the user"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "404", description = "User not found or assignment not found")
    })
    @DeleteMapping("/{userId}/roles/{roleId}")
    public ResponseEntity<Void> removeRoleFromUser(@PathVariable Long userId,
            @PathVariable Long roleId) {
        userRoleService.removeRoleFromUser(userId, roleId);
        return ResponseEntity.noContent().build();
    }
}
