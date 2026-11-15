package com.example.spring_boot_project_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.response.PublicListingResponse;
import com.example.spring_boot_project_api.service.PublicListingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Anonymous public endpoint for grouped multi-room listings. Only PUBLISHED
 * listings are served.
 */
@RestController
@RequestMapping("/api/public/listings")
@RequiredArgsConstructor
@Tag(name = "Public Listings", description = "Public grouped listing detail (no auth)")
public class PublicListingController {

    private final PublicListingService publicListingService;

    @Operation(summary = "Get a public listing",
            description = "Returns the full public listing detail for the requested id. "
                    + "Available without authentication for PUBLISHED listings.")
    @GetMapping("/{listingId}")
    public ResponseEntity<PublicListingResponse> getListing(@PathVariable Long listingId) {
        return ResponseEntity.ok(publicListingService.getPublicListing(listingId));
    }
}