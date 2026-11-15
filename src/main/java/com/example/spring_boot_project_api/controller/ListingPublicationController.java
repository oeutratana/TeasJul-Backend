package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.RejectListingRequest;
import com.example.spring_boot_project_api.dto.response.ListingResponse;
import com.example.spring_boot_project_api.enums.RoomPublicationStatus;
import com.example.spring_boot_project_api.service.ListingPublicationService;
import com.example.spring_boot_project_api.service.ListingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * HTTP layer for the grouped listing publication workflow.
 *
 * <p>Authorization is enforced server-side in {@link ListingPublicationService}:
 * owners/managers submit the listings they can access, while approve, reject and
 * unpublish are SUPER_ADMIN-only operations.</p>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Listing Publication",
        description = "Listing publication workflow: owner submission and SUPER_ADMIN review.")
public class ListingPublicationController {

    private final ListingPublicationService listingPublicationService;
    private final ListingService listingService;

    @Operation(summary = "Submit a listing for approval",
            description = "Owner or house manager submits a DRAFT or REJECTED listing. On success the "
                    + "listing moves to PENDING_APPROVAL.")
    @PostMapping("/api/listings/{listingId}/submit")
    public ResponseEntity<ListingResponse> submitForApproval(@PathVariable Long listingId) {
        return ResponseEntity.ok(toResponse(listingPublicationService.submitForApproval(listingId)));
    }

    @Operation(summary = "List listings by publication status (role-aware)",
            description = "OWNERs see their own listings, MANAGERs see managed houses, SUPER_ADMIN sees all. "
                    + "The status query parameter is optional.")
    @GetMapping("/api/listings/publication")
    public ResponseEntity<List<ListingResponse>> listListingsByPublicationStatus(
            @RequestParam(required = false) RoomPublicationStatus status) {
        return ResponseEntity.ok(listingPublicationService
                .getListingsByPublicationStatus(status).stream()
                .map(this::toResponse)
                .toList());
    }

    @Operation(summary = "Approve a pending listing (SUPER_ADMIN only)",
            description = "PENDING_APPROVAL -> PUBLISHED. The listing AND its attached rooms become visible "
                    + "on the public website.")
    @PostMapping("/api/admin/listings/{listingId}/approve")
    public ResponseEntity<ListingResponse> approve(@PathVariable Long listingId) {
        return ResponseEntity.ok(toResponse(listingPublicationService.approve(listingId)));
    }

    @Operation(summary = "Reject a pending listing (SUPER_ADMIN only)",
            description = "PENDING_APPROVAL -> REJECTED. A meaningful reason is required and is shown "
                    + "to the owner.")
    @PostMapping("/api/admin/listings/{listingId}/reject")
    public ResponseEntity<ListingResponse> reject(@PathVariable Long listingId,
            @Valid @RequestBody RejectListingRequest request) {
        return ResponseEntity.ok(toResponse(
                listingPublicationService.reject(listingId, request.reason())));
    }

    @Operation(summary = "Unpublish a published listing (SUPER_ADMIN only)",
            description = "PUBLISHED -> UNPUBLISHED. The listing and its rooms are removed from the "
                    + "public website.")
    @PostMapping("/api/admin/listings/{listingId}/unpublish")
    public ResponseEntity<ListingResponse> unpublish(@PathVariable Long listingId) {
        return ResponseEntity.ok(toResponse(listingPublicationService.unpublish(listingId)));
    }

    @Operation(summary = "List listings for review (SUPER_ADMIN only)",
            description = "When the status query parameter is omitted every listing is returned "
                    + "regardless of publication status.")
    @GetMapping("/api/admin/listings/publication")
    public ResponseEntity<List<ListingResponse>> listListingsForReview(
            @RequestParam(required = false) RoomPublicationStatus status) {
        return ResponseEntity.ok(listingPublicationService.getListingsForReview(status).stream()
                .map(this::toResponse)
                .toList());
    }

    private ListingResponse toResponse(com.example.spring_boot_project_api.model.Listing listing) {
        return listingService.toResponse(listing);
    }
}