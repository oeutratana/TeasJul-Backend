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

import com.example.spring_boot_project_api.dto.request.ListingUtilityRequest;
import com.example.spring_boot_project_api.dto.response.ListingUtilityResponse;
import com.example.spring_boot_project_api.service.ListingUtilityService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/listings/{listingId}/utilities")
@RequiredArgsConstructor
@Tag(name = "Listing Utilities",
        description = "Manage which utility types are billed for a listing")
public class ListingUtilityController {

    private final ListingUtilityService listingUtilityService;

    @Operation(summary = "List utilities assigned to a listing")
    @GetMapping
    public ResponseEntity<List<ListingUtilityResponse>> getListingUtilities(
            @PathVariable Long listingId) {
        return ResponseEntity.ok(listingUtilityService.getListingUtilities(listingId));
    }

    @Operation(summary = "Assign a utility type to a listing")
    @PostMapping("/{utilityTypeId}")
    public ResponseEntity<ListingUtilityResponse> assignUtility(
            @PathVariable Long listingId,
            @PathVariable Long utilityTypeId,
            @RequestBody(required = false) ListingUtilityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(listingUtilityService.assignUtility(listingId, utilityTypeId,
                        request == null ? false : request.isIncluded()));
    }

    @Operation(summary = "Remove a utility type from a listing")
    @DeleteMapping("/{utilityTypeId}")
    public ResponseEntity<Void> removeUtility(
            @PathVariable Long listingId,
            @PathVariable Long utilityTypeId) {
        listingUtilityService.removeUtility(listingId, utilityTypeId);
        return ResponseEntity.noContent().build();
    }
}