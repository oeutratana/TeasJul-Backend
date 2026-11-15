package com.example.spring_boot_project_api.controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.spring_boot_project_api.dto.request.RoomMeterImportRequest;
import com.example.spring_boot_project_api.dto.response.RoomMeterImportPreviewResponse;
import com.example.spring_boot_project_api.dto.response.RoomMeterImportResult;
import com.example.spring_boot_project_api.service.RoomMeterImportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/rooms/meters-import")
@RequiredArgsConstructor
@Tag(name = "Rooms + Meters Import",
        description = "Standalone CSV/Excel import that creates rooms and optionally meters "
                + "attached to them. Separate from the room-only and meter-only imports.")
public class RoomMeterImportController {

    private final RoomMeterImportService roomMeterImportService;

    @Operation(summary = "Parse and validate an uploaded rooms + meters CSV/Excel file",
            description = "Returns a row-level preview with errors/warnings. The house and room "
                    + "type are supplied from the UI; a meter install date is optional. Nothing is written.")
    @PostMapping("/preview")
    public ResponseEntity<RoomMeterImportPreviewResponse> previewImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "houseId", required = false) Long houseId,
            @RequestParam(value = "roomTypeId", required = false) Long roomTypeId,
            @RequestParam(value = "installDate", required = false) String installDate) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        return ResponseEntity.ok(roomMeterImportService.previewImport(
                houseId, roomTypeId, installDate, file.getOriginalFilename(), file.getBytes()));
    }

    @Operation(summary = "Confirm and transactionally import rooms + meters",
            description = "Re-validates every row server-side before writing. Rooms are created "
                    + "first and meters are attached to the created room ids. Single transaction; "
                    + "on any error nothing is persisted.")
    @PostMapping
    public ResponseEntity<RoomMeterImportResult> importRoomsWithMeters(
            @Valid @RequestBody RoomMeterImportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roomMeterImportService.importRoomsWithMeters(request));
    }
}