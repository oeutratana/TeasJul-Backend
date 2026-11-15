package com.example.spring_boot_project_api.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.PublicRoomQueryRequest;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.PublicRoomResponse;
import com.example.spring_boot_project_api.dto.response.PublicRoomTypeResponse;
import com.example.spring_boot_project_api.enums.Furnishing;
import com.example.spring_boot_project_api.enums.ListingScope;
import com.example.spring_boot_project_api.enums.PropertyType;
import com.example.spring_boot_project_api.enums.PublicRoomSort;
import com.example.spring_boot_project_api.service.PublicRoomService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Public, anonymous endpoints backing the public website's room browsing.
 *
 * <p>Only PUBLISHED rooms are exposed (read-only). These routes are permitted
 * without authentication in {@code SecurityConfig}.</p>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Public Rooms",
        description = "Public website browsing: published room listings and details (no auth).")
public class PublicRoomController {

    private final PublicRoomService publicRoomService;

    @Operation(summary = "List published rooms",
            description = "Paginated list of published, available rooms for the public website. "
                    + "Optional search and filters: cityProvince, propertyType, roomTypeId, listingScope, furnishing, "
                    + "minRent, maxRent, bedrooms, bathrooms, minAreaSqm, maxAreaSqm, capacity, availableFrom, amenities, utilities, sort.")
    @GetMapping("/api/public/rooms")
    public ResponseEntity<PageResponse<PublicRoomResponse>> listRooms(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String cityProvince,
            @RequestParam(required = false) PropertyType propertyType,
            @RequestParam(required = false) Long roomTypeId,
            @RequestParam(required = false) ListingScope listingScope,
            @RequestParam(required = false) Furnishing furnishing,
            @RequestParam(required = false) BigDecimal minRent,
            @RequestParam(required = false) BigDecimal maxRent,
            @RequestParam(required = false) Integer bedrooms,
            @RequestParam(required = false) Integer bathrooms,
            @RequestParam(required = false) BigDecimal minAreaSqm,
            @RequestParam(required = false) BigDecimal maxAreaSqm,
            @RequestParam(required = false) Integer capacity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate availableFrom,
            @RequestParam(required = false) List<String> amenities,
            @RequestParam(required = false) List<String> utilities,
            @RequestParam(required = false, defaultValue = "NEWEST") PublicRoomSort sort,
            @PageableDefault(size = 12) Pageable pageable) {
        PublicRoomQueryRequest query = new PublicRoomQueryRequest(
                search, cityProvince, propertyType, roomTypeId, listingScope, furnishing,
                minRent, maxRent, bedrooms, bathrooms, minAreaSqm, maxAreaSqm,
                capacity, availableFrom, amenities, utilities, sort);
        return ResponseEntity.ok(publicRoomService.listRooms(query, pageable));
    }

    @Operation(summary = "List public room types",
            description = "Distinct room types that appear in the public listing, sorted by name.")
    @GetMapping("/api/public/room-types")
    public ResponseEntity<List<PublicRoomTypeResponse>> listRoomTypes() {
        return ResponseEntity.ok(publicRoomService.getPublicRoomTypes());
    }

    @Operation(summary = "Get a published room",
            description = "Full public details of a single published room (with images and amenities).")
    @GetMapping("/api/public/rooms/{id}")
    public ResponseEntity<PublicRoomResponse> getRoom(@PathVariable Long id) {
        return ResponseEntity.ok(publicRoomService.getRoom(id));
    }

        @Operation(summary = "Get similar rooms", description = "Get up to 4 similar published House + RoomType groups from other houses.")
    @GetMapping("/api/public/rooms/{id}/similar")
    public ResponseEntity<List<PublicRoomResponse>> getSimilarRooms(@PathVariable Long id) {
        return ResponseEntity.ok(publicRoomService.getSimilarRooms(id));
    }
}
