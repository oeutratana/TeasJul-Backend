package com.example.spring_boot_project_api.controller;

import java.util.List;

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

import com.example.spring_boot_project_api.dto.request.CreateHouseRequest;
import com.example.spring_boot_project_api.dto.request.UpdateHouseRequest;
import com.example.spring_boot_project_api.dto.response.HouseResponse;
import com.example.spring_boot_project_api.dto.response.HouseSummaryResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.RentalContractResponse;
import com.example.spring_boot_project_api.dto.response.RoomResponse;
import com.example.spring_boot_project_api.dto.response.TenantResponse;
import com.example.spring_boot_project_api.enums.RentalContractStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.service.HouseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/houses")
@RequiredArgsConstructor
@Tag(name = "Houses", description = "House management endpoints (owner is resolved from the authenticated user)")
public class HouseController {

    private final HouseService houseService;

    @Operation(summary = "Create a new house",
            description = "Creates a house owned by the currently authenticated user. Name, slug and code must be unique per owner.")
    @PostMapping
    public ResponseEntity<HouseResponse> createHouse(@Valid @RequestBody CreateHouseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(houseService.createHouse(request));
    }

    @Operation(summary = "List my houses",
            description = "Returns all houses belonging to the currently authenticated user.")
    @GetMapping
    public ResponseEntity<List<HouseResponse>> getMyHouses() {
        return ResponseEntity.ok(houseService.getMyHouses());
    }

    @Operation(summary = "Get a house by id",
            description = "Returns the house with the given id if it belongs to the currently authenticated user.")
    @GetMapping("/{id}")
    public ResponseEntity<HouseResponse> getHouseById(@PathVariable Long id) {
        return ResponseEntity.ok(houseService.getHouseById(id));
    }

    @Operation(summary = "Get a house summary",
            description = "Returns statistics for the house with the given id if it belongs to the currently authenticated user.")
    @GetMapping("/{houseId}/summary")
    public ResponseEntity<HouseSummaryResponse> getHouseSummary(@PathVariable Long houseId) {
        return ResponseEntity.ok(houseService.getHouseSummary(houseId));
    }

    @Operation(summary = "List rooms of a house",
            description = "Returns the rooms of the house with the given id if it belongs to the currently authenticated user, "
                    + "with pagination, search and filter support.")
    @GetMapping("/{houseId}/rooms")
    public ResponseEntity<PageResponse<RoomResponse>> getHouseRooms(
            @PathVariable Long houseId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) RoomStatus status,
            @RequestParam(required = false) Long roomTypeId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(houseService.getHouseRooms(houseId, search, status, roomTypeId, pageable));
    }

    @Operation(summary = "List active tenants of a house",
            description = "Returns the currently active tenants of the house with the given id if it belongs to the "
                    + "currently authenticated user, with pagination and search support.")
    @GetMapping("/{houseId}/tenants")
    public ResponseEntity<PageResponse<TenantResponse>> getHouseTenants(
            @PathVariable Long houseId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(houseService.getHouseTenants(houseId, search, pageable));
    }

    @Operation(summary = "List contracts of a house",
            description = "Returns the contracts belonging to rooms of the house with the given id if it belongs to the "
                    + "currently authenticated user, with pagination, search and filter support.")
    @GetMapping("/{houseId}/contracts")
    public ResponseEntity<PageResponse<RentalContractResponse>> getHouseContracts(
            @PathVariable Long houseId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) RentalContractStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(houseService.getHouseContracts(houseId, search, status, pageable));
    }

    @Operation(summary = "Update a house",
            description = "Updates the house with the given id if it belongs to the currently authenticated user. Only provided fields are updated.")
    @PutMapping("/{id}")
    public ResponseEntity<HouseResponse> updateHouse(@PathVariable Long id,
            @Valid @RequestBody UpdateHouseRequest request) {
        return ResponseEntity.ok(houseService.updateHouse(id, request));
    }

    @Operation(summary = "Delete a house",
            description = "Deletes the house with the given id if it belongs to the currently authenticated user.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHouse(@PathVariable Long id) {
        houseService.deleteHouse(id);
        return ResponseEntity.noContent().build();
    }
}
