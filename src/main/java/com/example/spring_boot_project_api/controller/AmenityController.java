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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.AmenityRequest;
import com.example.spring_boot_project_api.dto.request.UpdateAmenityRequest;
import com.example.spring_boot_project_api.dto.response.AmenityResponse;
import com.example.spring_boot_project_api.enums.AmenityStatus;
import com.example.spring_boot_project_api.service.AmenityService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/amenities")
@RequiredArgsConstructor
@Tag(name = "Amenities", description = "Amenity management endpoints (owner is resolved from the authenticated user)")
public class AmenityController {

    private final AmenityService amenityService;

    @Operation(summary = "Create a new amenity",
            description = "Creates an amenity owned by the currently authenticated user. Name and code must be unique per owner.")
    @PostMapping
    public ResponseEntity<AmenityResponse> createAmenity(@Valid @RequestBody AmenityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(amenityService.createAmenity(request));
    }

    @Operation(summary = "List my amenities",
            description = "Returns all amenities belonging to the currently authenticated user. Optionally filter by status.")
    @GetMapping
    public ResponseEntity<List<AmenityResponse>> getMyAmenities(
            @RequestParam(required = false) AmenityStatus status) {
        if (status != null) {
            return ResponseEntity.ok(amenityService.getMyAmenitiesByStatus(status));
        }
        return ResponseEntity.ok(amenityService.getMyAmenities());
    }

    @Operation(summary = "Get an amenity by id",
            description = "Returns the amenity with the given id if it belongs to the currently authenticated user.")
    @GetMapping("/{id}")
    public ResponseEntity<AmenityResponse> getAmenityById(@PathVariable Long id) {
        return ResponseEntity.ok(amenityService.getAmenityById(id));
    }

    @Operation(summary = "Update an amenity",
            description = "Updates the amenity with the given id if it belongs to the currently authenticated user. Only provided fields are updated.")
    @PutMapping("/{id}")
    public ResponseEntity<AmenityResponse> updateAmenity(@PathVariable Long id,
            @Valid @RequestBody UpdateAmenityRequest request) {
        return ResponseEntity.ok(amenityService.updateAmenity(id, request));
    }

    @Operation(summary = "Delete an amenity",
            description = "Deletes the amenity with the given id if it belongs to the currently authenticated user.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAmenity(@PathVariable Long id) {
        amenityService.deleteAmenity(id);
        return ResponseEntity.noContent().build();
    }
}
