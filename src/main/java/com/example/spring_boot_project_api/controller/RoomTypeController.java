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
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.RoomTypeRequest;
import com.example.spring_boot_project_api.dto.response.RoomTypeResponse;
import com.example.spring_boot_project_api.service.RoomTypeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/room-types")
@RequiredArgsConstructor
@Tag(name = "Room Types", description = "Room type management endpoints")
public class RoomTypeController {

    private final RoomTypeService roomTypeService;

    @Operation(summary = "Create a new room type",
            description = "Creates a room type. Code must be unique.")
    @PostMapping
    public ResponseEntity<RoomTypeResponse> createRoomType(@Valid @RequestBody RoomTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomTypeService.createRoomType(request));
    }

    @Operation(summary = "List all room types",
            description = "Returns all room types.")
    @GetMapping
    public ResponseEntity<List<RoomTypeResponse>> getAllRoomTypes() {
        return ResponseEntity.ok(roomTypeService.getAllRoomTypes());
    }

    @Operation(summary = "Get a room type by id",
            description = "Returns the room type with the given id.")
    @GetMapping("/{id}")
    public ResponseEntity<RoomTypeResponse> getRoomTypeById(@PathVariable Long id) {
        return ResponseEntity.ok(roomTypeService.getRoomTypeById(id));
    }

    @Operation(summary = "List room types by owner",
            description = "Returns all room types belonging to the given owner.")
    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<RoomTypeResponse>> getRoomTypesByOwner(@PathVariable Long ownerId) {
        return ResponseEntity.ok(roomTypeService.getRoomTypesByOwner(ownerId));
    }

    @Operation(summary = "Update a room type",
            description = "Updates the room type with the given id. Code must remain unique.")
    @PutMapping("/{id}")
    public ResponseEntity<RoomTypeResponse> updateRoomType(@PathVariable Long id,
            @Valid @RequestBody RoomTypeRequest request) {
        return ResponseEntity.ok(roomTypeService.updateRoomType(id, request));
    }

    @Operation(summary = "Delete a room type",
            description = "Deletes the room type with the given id.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoomType(@PathVariable Long id) {
        roomTypeService.deleteRoomType(id);
        return ResponseEntity.noContent().build();
    }
}
