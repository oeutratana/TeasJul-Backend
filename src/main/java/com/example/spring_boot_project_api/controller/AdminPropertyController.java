package com.example.spring_boot_project_api.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

import com.example.spring_boot_project_api.dto.request.AdminPropertyPostRequest;
import com.example.spring_boot_project_api.dto.request.PropertyOwnerRequest;
import com.example.spring_boot_project_api.dto.response.AdminCatalogResponse;
import com.example.spring_boot_project_api.dto.response.AdminPropertyResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.PropertyOwnerResponse;
import com.example.spring_boot_project_api.enums.PropertyType;
import com.example.spring_boot_project_api.enums.RoomPublicationStatus;
import com.example.spring_boot_project_api.service.AdminPropertyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * HTTP layer for the ADMIN/SUPER_ADMIN property-posting flow, fully separate
 * from the OWNER portal. An admin posts a complete property (house / villa /
 * apartment / condo / room) on behalf of an owner that may be a TeasJul OWNER
 * account or a plain non-login owner record.
 *
 * <p>Authorization is enforced server-side in {@link AdminPropertyService}:
 * every operation requires the {@code ROLE_SUPER_ADMIN} role (never the generic
 * permission check, which auto-grants OWNER).</p>
 */
@RestController
@RequestMapping("/api/admin/properties")
@RequiredArgsConstructor
@Tag(name = "Admin Properties",
        description = "SUPER_ADMIN property posting: complete property posts, owner records and catalog.")
public class AdminPropertyController {

    private final AdminPropertyService adminPropertyService;

    @Operation(summary = "List admin-posted properties",
            description = "Paginated list of every posting managed through this flow, with optional "
                    + "server-side search by name/address, property type and publication status filter.")
    @GetMapping
    public ResponseEntity<PageResponse<AdminPropertyResponse>> listProperties(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) PropertyType propertyType,
            @RequestParam(required = false) RoomPublicationStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(adminPropertyService.list(search, propertyType, status, pageable));
    }

    @Operation(summary = "Catalog for the posting form",
            description = "Active amenities, utility types, non-login owner records and OWNER accounts "
                    + "used to render the admin posting form.")
    @GetMapping("/catalog")
    public ResponseEntity<AdminCatalogResponse> getCatalog() {
        return ResponseEntity.ok(adminPropertyService.getCatalog());
    }

    @Operation(summary = "Create a property post",
            description = "Creates (or reuses) the house, room type, room, amenities, utilities and "
                    + "nearby locations in one call. The owner is specified by exactly one of "
                    + "ownerUserId, ownerRecordId or an inline ownerRecord; no login account is ever "
                    + "created. When publish is true the post is created PUBLISHED and approved by the "
                    + "issuing SUPER_ADMIN; otherwise it is a DRAFT.")
    @PostMapping
    public ResponseEntity<AdminPropertyResponse> createProperty(
            @Valid @RequestBody AdminPropertyPostRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminPropertyService.create(request));
    }

    @Operation(summary = "Get a property post")
    @GetMapping("/{id}")
    public ResponseEntity<AdminPropertyResponse> getProperty(@PathVariable Long id) {
        return ResponseEntity.ok(adminPropertyService.get(id));
    }

    @Operation(summary = "Update a property post",
            description = "Updates the posted property in place. The backing house and room type are "
                    + "reused or created as needed, and amenities/utilities/nearby locations are "
                    + "replaced from the payload.")
    @PutMapping("/{id}")
    public ResponseEntity<AdminPropertyResponse> updateProperty(@PathVariable Long id,
            @Valid @RequestBody AdminPropertyPostRequest request) {
        return ResponseEntity.ok(adminPropertyService.update(id, request));
    }

    @Operation(summary = "List non-login owner records",
            description = "Paginated, searchable list of owner records managed via this flow. These "
                    + "are plain records with no login account.")
    @GetMapping("/owners")
    public ResponseEntity<PageResponse<PropertyOwnerResponse>> listPropertyOwners(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(adminPropertyService.listPropertyOwners(search, pageable));
    }

    @Operation(summary = "Create a non-login owner record",
            description = "Saves a plain owner record with no login account. Name is required.")
    @PostMapping("/owners")
    public ResponseEntity<PropertyOwnerResponse> createPropertyOwner(
            @Valid @RequestBody PropertyOwnerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminPropertyService.createPropertyOwner(request));
    }

    @Operation(summary = "Delete a non-login owner record",
            description = "Deletes an owner record. Records attached to one or more properties cannot "
                    + "be deleted.")
    @DeleteMapping("/owners/{id}")
    public ResponseEntity<Void> deletePropertyOwner(@PathVariable Long id) {
        adminPropertyService.deletePropertyOwner(id);
        return ResponseEntity.noContent().build();
    }
}