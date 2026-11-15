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

import com.example.spring_boot_project_api.dto.request.CreateNearbyLocationRequest;
import com.example.spring_boot_project_api.dto.request.UpdateNearbyLocationRequest;
import com.example.spring_boot_project_api.dto.response.NearbyLocationResponse;
import com.example.spring_boot_project_api.service.NearbyLocationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/houses/{houseId}/nearby-locations")
@RequiredArgsConstructor
@Tag(name = "Nearby Locations",
        description = "House-level nearby locations (POI) management")
public class NearbyLocationController {

    private final NearbyLocationService nearbyLocationService;

    @Operation(summary = "List nearby locations of a house",
            description = "Returns all nearby locations belonging to the requested house.")
    @GetMapping
    public ResponseEntity<List<NearbyLocationResponse>> getNearbyLocations(
            @PathVariable Long houseId) {
        return ResponseEntity.ok(nearbyLocationService.getNearbyLocations(houseId));
    }

    @Operation(summary = "Get a nearby location",
            description = "Returns the requested nearby location of the house.")
    @GetMapping("/{id}")
    public ResponseEntity<NearbyLocationResponse> getNearbyLocation(
            @PathVariable Long houseId,
            @PathVariable Long id) {
        return ResponseEntity.ok(nearbyLocationService.getNearbyLocation(houseId, id));
    }

    @Operation(summary = "Create a nearby location",
            description = "Adds a new nearby location (POI) to the requested house.")
    @PostMapping
    public ResponseEntity<NearbyLocationResponse> createNearbyLocation(
            @PathVariable Long houseId,
            @Valid @RequestBody CreateNearbyLocationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(nearbyLocationService.createNearbyLocation(houseId, request));
    }

    @Operation(summary = "Update a nearby location",
            description = "Updates the requested nearby location. Only provided (non-null) fields are changed.")
    @PutMapping("/{id}")
    public ResponseEntity<NearbyLocationResponse> updateNearbyLocation(
            @PathVariable Long houseId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateNearbyLocationRequest request) {
        return ResponseEntity.ok(nearbyLocationService.updateNearbyLocation(houseId, id, request));
    }

    @Operation(summary = "Delete a nearby location",
            description = "Deletes the requested nearby location.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNearbyLocation(
            @PathVariable Long houseId,
            @PathVariable Long id) {
        nearbyLocationService.deleteNearbyLocation(houseId, id);
        return ResponseEntity.noContent().build();
    }
}