package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.GrantHouseAccessRequest;
import com.example.spring_boot_project_api.dto.response.UserHouseAccessResponse;
import com.example.spring_boot_project_api.dto.response.UserResponse;
import com.example.spring_boot_project_api.service.UserHouseAccessService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/houses")
@RequiredArgsConstructor
@Tag(name = "User-House Access",
        description = "Grants and manages user access to houses. Only the authenticated OWNER of a house may grant, revoke or list its accesses (JWT required).")
public class UserHouseAccessController {

    private final UserHouseAccessService userHouseAccessService;

    @Operation(summary = "List MANAGER users available to grant access to a house",
            description = "OWNER only: returns all users with the MANAGER role who do not already have access to the given house.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of candidate users"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not the owner of the house"),
            @ApiResponse(responseCode = "404", description = "House not found")
    })
    @GetMapping("/{houseId}/access/candidates")
    public ResponseEntity<List<UserResponse>> getAccessCandidates(@PathVariable Long houseId) {
        return ResponseEntity.ok(userHouseAccessService.getAccessCandidates(houseId));
    }

    @Operation(summary = "Grant a user access to a house",
            description = "OWNER only: grants the target user access to the given house. Requires JWT authentication. "
                    + "Fails with 401 when unauthenticated, 403 when the authenticated user does not own the house, "
                    + "404 when the house or target user does not exist, 409 when the access already exists.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Access granted"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not the owner of the house"),
            @ApiResponse(responseCode = "404", description = "House or user not found"),
            @ApiResponse(responseCode = "409", description = "Access already exists")
    })
    @PostMapping("/{houseId}/access")
    public ResponseEntity<UserHouseAccessResponse> grantAccess(@PathVariable Long houseId,
            @Valid @RequestBody GrantHouseAccessRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userHouseAccessService.grantAccess(houseId, request));
    }

    @Operation(summary = "List users who have access to a house",
            description = "OWNER only: returns all users that have access to the given house. Requires JWT authentication. "
                    + "Fails with 401 when unauthenticated, 403 when the authenticated user does not own the house, "
                    + "404 when the house does not exist.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of users with access"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not the owner of the house"),
            @ApiResponse(responseCode = "404", description = "House not found")
    })
    @GetMapping("/{houseId}/access")
    public ResponseEntity<List<UserHouseAccessResponse>> getHouseUsers(@PathVariable Long houseId) {
        return ResponseEntity.ok(userHouseAccessService.getHouseUsers(houseId));
    }

    @Operation(summary = "Revoke a user's access to a house",
            description = "OWNER only: removes the relationship between the target user and the house; the user and the house are not deleted. "
                    + "Requires JWT authentication. Fails with 401 when unauthenticated, 403 when the authenticated user does not own the house, "
                    + "404 when the house or the access does not exist.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Access revoked"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not the owner of the house"),
            @ApiResponse(responseCode = "404", description = "House or access not found")
    })
    @DeleteMapping("/{houseId}/access/{userId}")
    public ResponseEntity<Void> revokeAccess(@PathVariable Long houseId, @PathVariable Long userId) {
        userHouseAccessService.revokeAccess(houseId, userId);
        return ResponseEntity.noContent().build();
    }
}
