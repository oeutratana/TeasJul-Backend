package com.example.spring_boot_project_api.controller;

import java.time.LocalDate;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
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

import com.example.spring_boot_project_api.dto.request.CreateUtilityRateRequest;
import com.example.spring_boot_project_api.dto.request.UpdateUtilityRateRequest;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.UtilityRateResponse;
import com.example.spring_boot_project_api.enums.UtilityRateStatus;
import com.example.spring_boot_project_api.service.UtilityRateService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/utility-rates")
@RequiredArgsConstructor
@Tag(name = "Utility Rates",
        description = "Utility rate management endpoints (owner is resolved from the authenticated user)")
public class UtilityRateController {

    private final UtilityRateService utilityRateService;

    @Operation(summary = "Create a utility rate",
            description = "Creates a utility rate owned by the authenticated user for a house and "
                    + "utility type that belong to the owner. Overlapping rate periods are rejected.")
    @PostMapping
    public ResponseEntity<UtilityRateResponse> createRate(@Valid @RequestBody CreateUtilityRateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(utilityRateService.createRate(request));
    }

    @Operation(summary = "List my utility rates",
            description = "Returns the utility rates belonging to the currently authenticated user, "
                    + "with pagination, house, utility type, status and effective date filters.")
    @GetMapping
    public ResponseEntity<PageResponse<UtilityRateResponse>> getRates(
            @RequestParam(required = false) Long houseId,
            @RequestParam(required = false) Long utilityTypeId,
            @RequestParam(required = false) UtilityRateStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveTo,
            @PageableDefault(size = 10, sort = "effectiveFrom", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(utilityRateService.getRates(houseId, utilityTypeId, status,
                effectiveFrom, effectiveTo, pageable));
    }

    @Operation(summary = "Get a utility rate by id",
            description = "Returns the utility rate with the given id if it belongs to the "
                    + "currently authenticated user.")
    @GetMapping("/{id}")
    public ResponseEntity<UtilityRateResponse> getRateById(@PathVariable Long id) {
        return ResponseEntity.ok(utilityRateService.getRateById(id));
    }

    @Operation(summary = "Update a utility rate",
            description = "Updates the utility rate with the given id if it belongs to the "
                    + "currently authenticated user. Only provided fields are updated, and "
                    + "overlapping rate periods are rejected.")
    @PutMapping("/{id}")
    public ResponseEntity<UtilityRateResponse> updateRate(@PathVariable Long id,
            @Valid @RequestBody UpdateUtilityRateRequest request) {
        return ResponseEntity.ok(utilityRateService.updateRate(id, request));
    }

    @Operation(summary = "Deactivate a utility rate",
            description = "Marks the utility rate with the given id as INACTIVE "
                    + "if it belongs to the currently authenticated user.")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateRate(@PathVariable Long id) {
        utilityRateService.deactivateRate(id);
        return ResponseEntity.noContent().build();
    }
}
