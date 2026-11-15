package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.CreateMaintenanceAssignmentRequest;
import com.example.spring_boot_project_api.dto.response.MaintenanceAssignmentResponse;
import com.example.spring_boot_project_api.service.MaintenanceAssignmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/maintenance-requests/{requestId}/assignments")
@RequiredArgsConstructor
@Tag(name = "Maintenance Assignments",
        description = "Maintenance assignment endpoints (request ownership is verified against the authenticated user)")
public class MaintenanceAssignmentController {

    private final MaintenanceAssignmentService assignmentService;

    @Operation(summary = "Assign a user to a maintenance request",
            description = "Creates an assignment for the given maintenance request if it belongs to the currently "
                    + "authenticated owner. The target user must exist and must have access to the request's house. "
                    + "A PENDING request automatically moves to IN_PROGRESS.")
    @PostMapping
    public ResponseEntity<MaintenanceAssignmentResponse> createAssignment(@PathVariable Long requestId,
            @Valid @RequestBody CreateMaintenanceAssignmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assignmentService.createAssignment(requestId, request));
    }

    @Operation(summary = "List assignments of a maintenance request",
            description = "Returns all assignments of the maintenance request if it belongs to the currently authenticated owner.")
    @GetMapping
    public ResponseEntity<List<MaintenanceAssignmentResponse>> getAssignments(@PathVariable Long requestId) {
        return ResponseEntity.ok(assignmentService.getAssignments(requestId));
    }

    @Operation(summary = "Complete an assignment",
            description = "Marks the assignment as completed if it belongs to the maintenance request "
                    + "of the currently authenticated owner.")
    @PatchMapping("/{assignmentId}/complete")
    public ResponseEntity<Void> completeAssignment(@PathVariable Long requestId,
            @PathVariable Long assignmentId) {
        assignmentService.completeAssignment(requestId, assignmentId);
        return ResponseEntity.noContent().build();
    }
}
