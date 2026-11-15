package com.example.spring_boot_project_api.controller;

import java.io.IOException;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.spring_boot_project_api.dto.request.CreateUtilityMeterRequest;
import com.example.spring_boot_project_api.dto.request.MeterImportRequest;
import com.example.spring_boot_project_api.dto.request.UpdateUtilityMeterRequest;
import com.example.spring_boot_project_api.dto.response.MeterImportPreviewResponse;
import com.example.spring_boot_project_api.dto.response.MeterImportResult;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.UtilityMeterResponse;
import com.example.spring_boot_project_api.enums.UtilityMeterStatus;
import com.example.spring_boot_project_api.service.MeterImportService;
import com.example.spring_boot_project_api.service.UtilityMeterService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/utility-meters")
@RequiredArgsConstructor
@Tag(name = "Utility Meters",
        description = "Utility meter management endpoints (owner is resolved from the authenticated user)")
public class UtilityMeterController {

    private final UtilityMeterService utilityMeterService;
    private final MeterImportService meterImportService;

    @Operation(summary = "Create a utility meter",
            description = "Creates a utility meter owned by the currently authenticated user. "
                    + "The house, room and utility type must belong to the owner.")
    @PostMapping
    public ResponseEntity<UtilityMeterResponse> createMeter(@Valid @RequestBody CreateUtilityMeterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(utilityMeterService.createMeter(request));
    }

    @Operation(summary = "List my utility meters",
            description = "Returns the utility meters belonging to the currently authenticated user, "
                    + "with pagination, search and filter support.")
    @GetMapping
    public ResponseEntity<PageResponse<UtilityMeterResponse>> getMeters(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long houseId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Long utilityTypeId,
            @RequestParam(required = false) UtilityMeterStatus status,
            @PageableDefault(size = 10, sort = { "createdAt", "id" }, direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(utilityMeterService.getMeters(search, houseId, roomId,
                utilityTypeId, status, pageable));
    }

    @Operation(summary = "Get a utility meter by id",
            description = "Returns the utility meter with the given id if it belongs to the "
                    + "currently authenticated user.")
    @GetMapping("/{id}")
    public ResponseEntity<UtilityMeterResponse> getMeterById(@PathVariable Long id) {
        return ResponseEntity.ok(utilityMeterService.getMeterById(id));
    }

    @Operation(summary = "Parse and validate an uploaded utility meter CSV/Excel file",
            description = "Returns a row-level preview with errors/warnings. Nothing is written.")
    @PostMapping("/import/preview")
    public ResponseEntity<MeterImportPreviewResponse> previewImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "houseId", required = false) Long houseId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        return ResponseEntity.ok(meterImportService.previewImport(
                houseId, file.getOriginalFilename(), file.getBytes()));
    }

    @Operation(summary = "Confirm and transactionally import utility meters",
            description = "Re-validates every row server-side before writing. Single transaction; "
                    + "on any error nothing is persisted.")
    @PostMapping("/import")
    public ResponseEntity<MeterImportResult> importMeters(
            @Valid @RequestBody MeterImportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(meterImportService.importMeters(request));
    }

    @Operation(summary = "Update a utility meter",
            description = "Updates the utility meter with the given id if it belongs to the "
                    + "currently authenticated user. Only provided fields are updated.")
    @PutMapping("/{id}")
    public ResponseEntity<UtilityMeterResponse> updateMeter(@PathVariable Long id,
            @Valid @RequestBody UpdateUtilityMeterRequest request) {
        return ResponseEntity.ok(utilityMeterService.updateMeter(id, request));
    }

    @Operation(summary = "Deactivate a utility meter",
            description = "Marks the utility meter with the given id as INACTIVE "
                    + "if it belongs to the currently authenticated user.")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateMeter(@PathVariable Long id) {
        utilityMeterService.deactivateMeter(id);
        return ResponseEntity.noContent().build();
    }
}
