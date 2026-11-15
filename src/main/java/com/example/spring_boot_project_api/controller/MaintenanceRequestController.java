package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.ChangeMaintenanceRequestStatusRequest;
import com.example.spring_boot_project_api.dto.request.CreateMaintenanceRequestRequest;
import com.example.spring_boot_project_api.dto.request.CreateTenantMaintenanceRequestRequest;
import com.example.spring_boot_project_api.dto.request.UpdateMaintenanceRequestRequest;
import com.example.spring_boot_project_api.dto.response.MaintenanceRequestResponse;
import com.example.spring_boot_project_api.service.MaintenanceRequestService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/maintenance-requests")
@RequiredArgsConstructor
@Tag(name = "Maintenance Requests",
        description = "Maintenance request endpoints (owner is resolved from the authenticated user)")
public class MaintenanceRequestController {

    private final MaintenanceRequestService requestService;

    @Operation(summary = "Create a maintenance request",
            description = "Creates a maintenance request for a room of one of the authenticated owner's houses. "
                    + "The room must belong to the selected house and optional tenant/category must belong to the owner. "
                    + "New requests start with status PENDING.")
    @PostMapping
    public ResponseEntity<MaintenanceRequestResponse> createRequest(
            @Valid @RequestBody CreateMaintenanceRequestRequest request) {
        return ResponseEntity.ok(requestService.createRequest(request));
    }

    @Operation(summary = "List my maintenance requests",
            description = "Returns all maintenance requests belonging to the currently authenticated user.")
    @GetMapping
    public ResponseEntity<List<MaintenanceRequestResponse>> getMyRequests() {
        return ResponseEntity.ok(requestService.getMyRequests());
    }

    @Operation(summary = "List my maintenance requests (tenant)",
            description = "Returns the maintenance requests reported by the tenant profile linked "
                    + "to the currently authenticated user.")
    @GetMapping("/my")
    public ResponseEntity<List<MaintenanceRequestResponse>> getMyRequestsAsTenant() {
        return ResponseEntity.ok(requestService.getMyRequestsAsTenant());
    }

    @Operation(summary = "Create a maintenance request (tenant)",
            description = "Creates a maintenance request for the room of the current tenant's active rental contract. "
                    + "Owner, house and room are resolved from the tenant's contract.")
    @PostMapping("/my")
    public ResponseEntity<MaintenanceRequestResponse> createMyRequest(
            @Valid @RequestBody CreateTenantMaintenanceRequestRequest request) {
        return ResponseEntity.ok(requestService.createMyRequest(request));
    }

    @Operation(summary = "Get a maintenance request by id",
            description = "Returns the maintenance request with the given id if it belongs to the currently authenticated user.")
    @GetMapping("/{id}")
    public ResponseEntity<MaintenanceRequestResponse> getRequestById(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.getRequestById(id));
    }

    @Operation(summary = "Update a maintenance request",
            description = "Updates the title, description or priority of the maintenance request "
                    + "if it belongs to the currently authenticated user. Only provided fields are updated. "
                    + "Status changes must use the dedicated status endpoint.")
    @PutMapping("/{id}")
    public ResponseEntity<MaintenanceRequestResponse> updateRequest(@PathVariable Long id,
            @Valid @RequestBody UpdateMaintenanceRequestRequest request) {
        return ResponseEntity.ok(requestService.updateRequest(id, request));
    }

    @Operation(summary = "Change maintenance request status",
            description = "Moves the maintenance request through its allowed status flow: "
                    + "PENDING -> IN_PROGRESS -> RESOLVED, or PENDING -> CANCELLED. Other transitions are rejected.")
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> changeStatus(@PathVariable Long id,
            @Valid @RequestBody ChangeMaintenanceRequestStatusRequest request) {
        requestService.changeStatus(id, request);
        return ResponseEntity.noContent().build();
    }
}
