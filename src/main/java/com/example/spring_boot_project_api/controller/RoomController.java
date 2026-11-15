package com.example.spring_boot_project_api.controller;

import java.io.IOException;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.spring_boot_project_api.dto.request.BulkRoomAmenityRequest;
import com.example.spring_boot_project_api.dto.request.BulkRoomUtilityRequest;
import com.example.spring_boot_project_api.dto.request.RoomImportRequest;
import com.example.spring_boot_project_api.dto.request.RoomRepuest;
import com.example.spring_boot_project_api.dto.response.BulkAssignmentResponse;
import com.example.spring_boot_project_api.dto.response.RoomImportPreviewResponse;
import com.example.spring_boot_project_api.dto.response.RoomImportResult;
import com.example.spring_boot_project_api.dto.response.RoomResponse;
import com.example.spring_boot_project_api.service.RoomAmenityService;
import com.example.spring_boot_project_api.service.RoomImportService;
import com.example.spring_boot_project_api.service.RoomService;
import com.example.spring_boot_project_api.service.RoomUtilityService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Tag(name = "Rooms", description = "Room management endpoints")
public class RoomController {

    private final RoomService roomService;
    private final RoomImportService roomImportService;
    private final RoomUtilityService roomUtilityService;
    private final RoomAmenityService roomAmenityService;

    @Operation(summary = "Create a new room",
            description = "Creates a room. Room number must be unique within its house.")
    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody RoomRepuest requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.createRoom(requestDto));
    }

    @Operation(summary = "List all rooms",
            description = "Returns all rooms.")
    @GetMapping
    public ResponseEntity<List<RoomResponse>> getAllRooms() {
        return ResponseEntity.ok(roomService.getAllRooms());
    }

    @Operation(summary = "Get a room by id",
            description = "Returns the room with the given id.")
    @GetMapping("/{id}")
    public ResponseEntity<RoomResponse> getRoomById(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getRoomById(id));
    }

    @Operation(summary = "List rooms by house",
            description = "Returns all rooms belonging to the given house.")
    @GetMapping("/house/{houseId}")
    public ResponseEntity<List<RoomResponse>> getRoomsByHouse(@PathVariable Long houseId) {
        return ResponseEntity.ok(roomService.getRoomsByHouse(houseId));
    }

    @Operation(summary = "Parse and validate an uploaded room CSV/Excel file",
            description = "Returns a row-level preview with errors/warnings. Nothing is written.")
    @PostMapping("/import/preview")
    public ResponseEntity<RoomImportPreviewResponse> previewImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "houseId", required = false) Long houseId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        return ResponseEntity.ok(roomImportService.previewImport(
                houseId, file.getOriginalFilename(), file.getBytes()));
    }

    @Operation(summary = "Confirm and transactionally import rooms",
            description = "Re-validates every row server-side before writing. Single transaction; "
                    + "on any error nothing is persisted.")
    @PostMapping("/import")
    public ResponseEntity<RoomImportResult> importRooms(
            @Valid @RequestBody RoomImportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roomImportService.importRooms(request));
    }

    @Operation(summary = "Bulk assign utilities to many rooms",
            description = "Assigns the given utility types to all given rooms at once. "
                    + "Existing ACTIVE assignments are skipped and reported in the summary. "
                    + "Rooms or utility types that cannot be assigned become per-item failures.")
    @PostMapping("/bulk-utilities")
    public ResponseEntity<BulkAssignmentResponse> bulkAssignUtilities(
            @Valid @RequestBody BulkRoomUtilityRequest request) {
        return ResponseEntity.ok(roomUtilityService.bulkAssignUtilities(request));
    }

    @Operation(summary = "Bulk assign amenities to many rooms",
            description = "Assigns the given amenities to all given rooms at once. "
                    + "Existing assignments are skipped and reported in the summary. "
                    + "Rooms or amenities that cannot be assigned become per-item failures.")
    @PostMapping("/bulk-amenities")
    public ResponseEntity<BulkAssignmentResponse> bulkAssignAmenities(
            @Valid @RequestBody BulkRoomAmenityRequest request) {
        return ResponseEntity.ok(roomAmenityService.bulkAssignAmenities(request));
    }

    @Operation(summary = "Update a room",
            description = "Updates the room with the given id.")
    @PutMapping("/{id}")
    public ResponseEntity<RoomResponse> updateRoom(@PathVariable Long id,
            @Valid @RequestBody RoomRepuest requestDto) {
        return ResponseEntity.ok(roomService.updateRoom(id, requestDto));
    }

    @Operation(summary = "Delete a room",
            description = "Deletes the room with the given id.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }
}
