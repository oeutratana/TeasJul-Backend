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

import com.example.spring_boot_project_api.dto.response.ListingAmenityResponse;
import com.example.spring_boot_project_api.service.ListingAmenityService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/listings/{listingId}/amenities")
@RequiredArgsConstructor
@Tag(name = "Listing Amenities",
        description = "Manage which amenities are advertised on a listing")
public class ListingAmenityController {

    private final ListingAmenityService listingAmenityService;

    @Operation(summary = "List amenities assigned to a listing")
    @GetMapping
    public ResponseEntity<List<ListingAmenityResponse>> getListingAmenities(
            @PathVariable Long listingId) {
        return ResponseEntity.ok(listingAmenityService.getListingAmenities(listingId));
    }

    @Operation(summary = "Assign an amenity to a listing")
    @PostMapping("/{amenityId}")
    public ResponseEntity<ListingAmenityResponse> assignAmenity(
            @PathVariable Long listingId,
            @PathVariable Long amenityId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(listingAmenityService.assignAmenity(listingId, amenityId));
    }

    @Operation(summary = "Remove an amenity from a listing")
    @DeleteMapping("/{amenityId}")
    public ResponseEntity<Void> removeAmenity(
            @PathVariable Long listingId,
            @PathVariable Long amenityId) {
        listingAmenityService.removeAmenity(listingId, amenityId);
        return ResponseEntity.noContent().build();
    }
}