package com.example.spring_boot_project_api.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.CreateUtilityReadingRequest;
import com.example.spring_boot_project_api.dto.request.MonthlyReadingBulkRequest;
import com.example.spring_boot_project_api.dto.response.BulkUtilityReadingResponse;
import com.example.spring_boot_project_api.dto.response.MonthlyReadingRowResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.UtilityReadingResponse;
import com.example.spring_boot_project_api.enums.ReadingStatusFilter;
import com.example.spring_boot_project_api.service.UtilityReadingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/utility-readings")
@RequiredArgsConstructor
@Tag(name = "Utility Readings",
        description = "Utility reading endpoints (owner is resolved from the authenticated user)")
public class UtilityReadingController {

    private final UtilityReadingService utilityReadingService;

    @Operation(summary = "Create a utility reading",
            description = "Creates a reading for a meter owned by the authenticated user. "
                    + "Usage is calculated on the server; the current reading must not be less "
                    + "than the previous reading.")
    @PostMapping
    public ResponseEntity<UtilityReadingResponse> createReading(
            @Valid @RequestBody CreateUtilityReadingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(utilityReadingService.createReading(request));
    }

    @Operation(summary = "List my utility readings",
            description = "Returns the utility readings for meters belonging to the authenticated "
                    + "user, with pagination, meter filter and date range support.")
    @GetMapping
    public ResponseEntity<PageResponse<UtilityReadingResponse>> getReadings(
            @RequestParam(required = false) Long meterId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 10, sort = "readingDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(utilityReadingService.getReadings(meterId, fromDate, toDate, pageable));
    }

    @Operation(summary = "Monthly meter-reading sheet",
            description = "Returns one row per ACTIVE meter of the selected house scope for the "
                    + "given billing month. Each row carries the previous reading (last reading "
                    + "before the month) and the current reading already recorded inside the "
                    + "month, if any. WHEN houseId IS OMITTED the whole accessible house scope of "
                    + "the authenticated user is used. Supports optional floor, search (room "
                    + "number / tenant name / meter number) and reading-status (ENTERED / "
                    + "NOT_ENTERED) filters evaluated on persisted data.")
    @GetMapping("/monthly-sheet")
    public ResponseEntity<List<MonthlyReadingRowResponse>> getMonthlySheet(
            @RequestParam(required = false) Long houseId,
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam(required = false) Integer floor,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ReadingStatusFilter status) {
        return ResponseEntity.ok(utilityReadingService.getMonthlySheet(
                houseId, year, month, floor, search, status));
    }

    @Operation(summary = "Floors with meters for the monthly sheet",
            description = "Distinct floor numbers (sorted ascending) that currently have ACTIVE "
                    + "meters within the selected house scope. When houseId is omitted the whole "
                    + "accessible house scope of the authenticated user is used.")
    @GetMapping("/floors")
    public ResponseEntity<List<Integer>> getMonthlySheetFloors(
            @RequestParam(required = false) Long houseId) {
        return ResponseEntity.ok(utilityReadingService.getMonthlySheetFloors(houseId));
    }

    @Operation(summary = "Get a utility reading by id",
            description = "Returns the utility reading with the given id if it belongs to a meter "
                    + "owned by the currently authenticated user.")
    @GetMapping("/{id}")
    public ResponseEntity<UtilityReadingResponse> getReadingById(@PathVariable Long id) {
        return ResponseEntity.ok(utilityReadingService.getReadingById(id));
    }

    @Operation(summary = "Save the monthly meter-reading sheet",
            description = "Creates or updates the readings for a whole house in one call. "
                    + "Usage is recalculated on the server from the last reading before the month.")
    @PostMapping("/bulk")
    public ResponseEntity<BulkUtilityReadingResponse> createReadingsBulk(
            @Valid @RequestBody MonthlyReadingBulkRequest request) {
        return ResponseEntity.ok(utilityReadingService.createReadingsBulk(request));
    }
}
