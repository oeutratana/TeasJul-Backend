package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.AdminBulkCreateRoomRequest;
import com.example.spring_boot_project_api.dto.request.AdminCreateRoomRequest;
import com.example.spring_boot_project_api.dto.request.CreateHouseRequest;
import com.example.spring_boot_project_api.dto.request.RoomTypeRequest;
import com.example.spring_boot_project_api.dto.response.HouseResponse;
import com.example.spring_boot_project_api.dto.response.RoomResponse;
import com.example.spring_boot_project_api.dto.response.RoomTypeResponse;
import com.example.spring_boot_project_api.service.AdminRoomService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * HTTP layer for the SUPER_ADMIN "post a room" flow. Rooms are always created
 * for an existing OWNER; the houses and room types returned belong to that
 * owner only.
 *
 * <p>Authorization is enforced server-side in {@link AdminRoomService}: every
 * operation requires the {@code ROLE_SUPER_ADMIN} role.</p>
 */
@RestController
@RequestMapping("/api/admin/owners/{ownerId}")
@RequiredArgsConstructor
@Tag(name = "Admin Rooms",
        description = "SUPER_ADMIN room posting for an OWNER: houses, room types and create room.")
public class AdminRoomController {

    private final AdminRoomService adminRoomService;

    @Operation(summary = "List houses of an owner",
            description = "Houses that belong to the given OWNER, newest first.")
    @GetMapping("/houses")
    public ResponseEntity<List<HouseResponse>> getOwnerHouses(@PathVariable Long ownerId) {
        return ResponseEntity.ok(adminRoomService.getOwnerHouses(ownerId));
    }

    @Operation(summary = "List room types of an owner",
            description = "Room types that belong to the given OWNER.")
    @GetMapping("/room-types")
    public ResponseEntity<List<RoomTypeResponse>> getOwnerRoomTypes(@PathVariable Long ownerId) {
        return ResponseEntity.ok(adminRoomService.getOwnerRoomTypes(ownerId));
    }

    @Operation(summary = "Create a room for an owner",
            description = "Creates a room for the given OWNER. When publishImmediately is true the "
                    + "room is created as PUBLISHED and approved by the current admin; otherwise it is "
                    + "created as DRAFT.")
    @PostMapping("/rooms")
    public ResponseEntity<RoomResponse> createRoomForOwner(@PathVariable Long ownerId,
            @Valid @RequestBody AdminCreateRoomRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminRoomService.createRoomForOwner(ownerId, request));
    }

    @Operation(summary = "Create multiple rooms for an owner",
            description = "Creates one or more physical rooms for the given OWNER in a single call. "
                    + "Every entry must reference the owner's own house and room type. When "
                    + "publishImmediately is true every room is created as PUBLISHED and approved by "
                    + "the current admin; otherwise they are all created as DRAFT.")
    @PostMapping("/rooms/bulk")
    public ResponseEntity<List<RoomResponse>> createRoomsForOwner(@PathVariable Long ownerId,
            @Valid @RequestBody AdminBulkCreateRoomRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminRoomService.createRoomsForOwner(ownerId, request));
    }

    @Operation(summary = "Create a house for an owner",
            description = "Creates a house owned by the given OWNER without requiring them to log in. "
                    + "Name, slug and code must be unique per owner.")
    @PostMapping("/houses")
    public ResponseEntity<HouseResponse> createHouseForOwner(@PathVariable Long ownerId,
            @Valid @RequestBody CreateHouseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminRoomService.createHouseForOwner(ownerId, request));
    }

    @Operation(summary = "Create a room type for an owner",
            description = "Creates a room type owned by the given OWNER without requiring them to log in. "
                    + "Code must be unique.")
    @PostMapping("/room-types")
    public ResponseEntity<RoomTypeResponse> createRoomTypeForOwner(@PathVariable Long ownerId,
            @Valid @RequestBody RoomTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminRoomService.createRoomTypeForOwner(ownerId, request));
    }
}