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

import com.example.spring_boot_project_api.dto.response.RoomAmenityResponse;
import com.example.spring_boot_project_api.service.RoomAmenityService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/rooms/{roomId}/amenities")
@RequiredArgsConstructor
@Tag(name = "Room Amenities",
        description = "Assigns amenities to rooms. Only the authenticated owner of an amenity may assign or remove it (JWT required).")
public class RoomAmenityController {

    private final RoomAmenityService roomAmenityService;

    @Operation(summary = "Assign an amenity to a room",
            description = "Assigns the given amenity to the given room. Fails with 404 when the amenity does not exist, "
                    + "403 when the authenticated user does not own the amenity, 409 when already assigned.")
    @PostMapping("/{amenityId}")
    public ResponseEntity<RoomAmenityResponse> assignAmenity(@PathVariable Long roomId,
            @PathVariable Long amenityId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roomAmenityService.assignAmenityToRoom(roomId, amenityId));
    }

    @Operation(summary = "List amenities of a room",
            description = "Returns all amenities assigned to the given room.")
    @GetMapping
    public ResponseEntity<List<RoomAmenityResponse>> getRoomAmenities(@PathVariable Long roomId) {
        return ResponseEntity.ok(roomAmenityService.getRoomAmenities(roomId));
    }

    @Operation(summary = "Remove an amenity from a room",
            description = "Removes the assignment between the given amenity and room; the amenity itself is not deleted. "
                    + "Fails with 404 when the assignment does not exist.")
    @DeleteMapping("/{amenityId}")
    public ResponseEntity<Void> removeAmenity(@PathVariable Long roomId,
            @PathVariable Long amenityId) {
        roomAmenityService.removeAmenityFromRoom(roomId, amenityId);
        return ResponseEntity.noContent().build();
    }
}
