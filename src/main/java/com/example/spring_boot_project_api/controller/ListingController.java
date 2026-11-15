package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

import com.example.spring_boot_project_api.dto.request.ListingRequest;
import com.example.spring_boot_project_api.dto.response.ListingBlueprintResponse;
import com.example.spring_boot_project_api.dto.response.ListingResponse;
import com.example.spring_boot_project_api.dto.response.ListingRoomResponse;
import com.example.spring_boot_project_api.dto.response.PublicListingResponse;
import com.example.spring_boot_project_api.enums.RoomPublicationStatus;
import com.example.spring_boot_project_api.service.ListingService;
import com.example.spring_boot_project_api.service.PublicListingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Owner/manager grouped-listing CRUD, room selection, blueprint and preview.
 *
 * <p>Authorization is enforced server-side in {@code ListingService} /
 * {@code ListingAccessService}: content creation and editing is OWNER-only, the
 * rest requires {@code LISTING_MANAGE} plus access to the listing's house.</p>
 */
@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
@Tag(name = "Listings",
        description = "Owner/manager grouped multi-room listings")
public class ListingController {

    private final ListingService listingService;
    private final PublicListingService publicListingService;

    @Operation(summary = "List my listings",
            description = "Role-aware: owner sees own listings, manager sees managed houses, "
                    + "SUPER_ADMIN sees every listing.")
    @GetMapping
    public ResponseEntity<List<ListingResponse>> getMyListings() {
        return ResponseEntity.ok(listingService.getMyListings());
    }


    @Operation(summary = "Get a listing by id")
    @GetMapping("/{listingId}")
    public ResponseEntity<ListingResponse> getListing(@PathVariable Long listingId) {
        return ResponseEntity.ok(listingService.getListing(listingId));
    }

    @Operation(summary = "Get or create the listing for a house + room type",
            description = "Returns the existing listing for the pair, or creates a DRAFT one. OWNER only.")
    @GetMapping("/by-house-room-type")
    public ResponseEntity<ListingResponse> getOrCreate(
            @RequestParam Long houseId, @RequestParam Long roomTypeId) {
        return ResponseEntity.ok(listingService.getOrCreate(houseId, roomTypeId));
    }

    @Operation(summary = "List rooms eligible to attach to a listing",
            description = "Same house + room type, AVAILABLE status, not attached to another listing.")
    @GetMapping("/{listingId}/eligible-rooms")
    public ResponseEntity<List<ListingRoomResponse>> getEligibleRooms(
            @PathVariable Long listingId) {
        return ResponseEntity.ok(listingService.getEligibleRooms(listingId));
    }

    @Operation(summary = "Create a listing")
    @PostMapping
    public ResponseEntity<ListingResponse> create(@Valid @RequestBody ListingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(listingService.create(request));
    }

    @Operation(summary = "Update a listing")
    @PutMapping("/{listingId}")
    public ResponseEntity<ListingResponse> update(@PathVariable Long listingId,
            @Valid @RequestBody ListingRequest request) {
        return ResponseEntity.ok(listingService.update(listingId, request));
    }

    @Operation(summary = "Delete a listing",
            description = "Detaches all rooms, deletes Cloudinary assets and the listing row.")
    @DeleteMapping("/{listingId}")
    public ResponseEntity<Void> delete(@PathVariable Long listingId) {
        listingService.delete(listingId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get the listing blueprint")
    @GetMapping("/{listingId}/blueprint")
    public ResponseEntity<ListingBlueprintResponse> getBlueprint(@PathVariable Long listingId) {
        return ResponseEntity.ok(listingService.getBlueprint(listingId));
    }

    @Operation(summary = "Upload the listing blueprint/floor plan",
            description = "JPEG, PNG and WEBP only, max 5 MB. Replaces any existing blueprint.")
    @PostMapping(value = "/{listingId}/blueprint", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ListingBlueprintResponse> uploadBlueprint(@PathVariable Long listingId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(listingService.uploadBlueprint(listingId, file));
    }

    @Operation(summary = "Delete the listing blueprint")
    @DeleteMapping("/{listingId}/blueprint")
    public ResponseEntity<Void> deleteBlueprint(@PathVariable Long listingId) {
        listingService.deleteBlueprint(listingId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Preview the public listing",
            description = "Renders the exact public listing shape for the requested id "
                    + "(any publication status). Used by the builder's 'Preview Public Listing' step.")
    @GetMapping("/{listingId}/preview")
    public ResponseEntity<PublicListingResponse> preview(@PathVariable Long listingId) {
        return ResponseEntity.ok(publicListingService.getPreview(listingId));
    }
}