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

import com.example.spring_boot_project_api.dto.request.CreateMaintenanceCostRequest;
import com.example.spring_boot_project_api.dto.request.UpdateMaintenanceCostRequest;
import com.example.spring_boot_project_api.dto.response.MaintenanceCostResponse;
import com.example.spring_boot_project_api.service.MaintenanceCostService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/maintenance-requests/{requestId}/costs")
@RequiredArgsConstructor
@Tag(name = "Maintenance Costs",
        description = "Maintenance cost endpoints (request ownership is verified against the authenticated user)")
public class MaintenanceCostController {

    private final MaintenanceCostService costService;

    @Operation(summary = "Add a cost record to a maintenance request",
            description = "Adds a cost record (labor, spare part, transport, material or other) to the given "
                    + "maintenance request if it belongs to the currently authenticated owner. "
                    + "The total amount is always calculated on the backend as quantity x unit price.")
    @PostMapping
    public ResponseEntity<MaintenanceCostResponse> createCost(@PathVariable Long requestId,
            @Valid @RequestBody CreateMaintenanceCostRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(costService.createCost(requestId, request));
    }

    @Operation(summary = "List cost records of a maintenance request",
            description = "Returns all cost records of the maintenance request if it belongs to "
                    + "the currently authenticated owner.")
    @GetMapping
    public ResponseEntity<List<MaintenanceCostResponse>> getCosts(@PathVariable Long requestId) {
        return ResponseEntity.ok(costService.getCosts(requestId));
    }

    @Operation(summary = "Update a cost record",
            description = "Updates a cost record if it belongs to the maintenance request of the currently "
                    + "authenticated owner. Only provided fields are updated and the total amount "
                    + "is recalculated on the backend.")
    @PutMapping("/{costId}")
    public ResponseEntity<MaintenanceCostResponse> updateCost(@PathVariable Long requestId,
            @PathVariable Long costId, @Valid @RequestBody UpdateMaintenanceCostRequest request) {
        return ResponseEntity.ok(costService.updateCost(requestId, costId, request));
    }

    @Operation(summary = "Delete a cost record",
            description = "Deletes a cost record if it belongs to the maintenance request "
                    + "of the currently authenticated owner.")
    @DeleteMapping("/{costId}")
    public ResponseEntity<Void> deleteCost(@PathVariable Long requestId,
            @PathVariable Long costId) {
        costService.deleteCost(requestId, costId);
        return ResponseEntity.noContent().build();
    }
}
