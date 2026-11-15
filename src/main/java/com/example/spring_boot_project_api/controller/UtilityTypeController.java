package com.example.spring_boot_project_api.controller;

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

import com.example.spring_boot_project_api.dto.request.CreateUtilityTypeRequest;
import com.example.spring_boot_project_api.dto.request.UpdateUtilityTypeRequest;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.UtilityTypeResponse;
import com.example.spring_boot_project_api.enums.UtilityTypeStatus;
import com.example.spring_boot_project_api.service.UtilityTypeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/utility-types")
@RequiredArgsConstructor
@Tag(name = "Utility Types",
        description = "Utility type management endpoints (owner is resolved from the authenticated user)")
public class UtilityTypeController {

    private final UtilityTypeService utilityTypeService;

    @Operation(summary = "Create a utility type",
            description = "Creates a utility type owned by the currently authenticated user. "
                    + "Name and code must be unique per owner.")
    @PostMapping
    public ResponseEntity<UtilityTypeResponse> createType(@Valid @RequestBody CreateUtilityTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(utilityTypeService.createType(request));
    }

    @Operation(summary = "List my utility types",
            description = "Returns the utility types belonging to the currently authenticated user, "
                    + "with pagination, search and status filtering.")
    @GetMapping
    public ResponseEntity<PageResponse<UtilityTypeResponse>> getTypes(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UtilityTypeStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(utilityTypeService.getTypes(search, status, pageable));
    }

    @Operation(summary = "Get a utility type by id",
            description = "Returns the utility type with the given id if it belongs to the "
                    + "currently authenticated user.")
    @GetMapping("/{id}")
    public ResponseEntity<UtilityTypeResponse> getTypeById(@PathVariable Long id) {
        return ResponseEntity.ok(utilityTypeService.getTypeById(id));
    }

    @Operation(summary = "Update a utility type",
            description = "Updates the utility type with the given id if it belongs to the "
                    + "currently authenticated user. Only provided fields are updated.")
    @PutMapping("/{id}")
    public ResponseEntity<UtilityTypeResponse> updateType(@PathVariable Long id,
            @Valid @RequestBody UpdateUtilityTypeRequest request) {
        return ResponseEntity.ok(utilityTypeService.updateType(id, request));
    }

    @Operation(summary = "Deactivate a utility type",
            description = "Marks the utility type with the given id as INACTIVE "
                    + "if it belongs to the currently authenticated user.")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateType(@PathVariable Long id) {
        utilityTypeService.deactivateType(id);
        return ResponseEntity.noContent().build();
    }
}
