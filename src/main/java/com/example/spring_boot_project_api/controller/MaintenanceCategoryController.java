package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.CreateMaintenanceCategoryRequest;
import com.example.spring_boot_project_api.dto.request.UpdateMaintenanceCategoryRequest;
import com.example.spring_boot_project_api.dto.response.MaintenanceCategoryResponse;
import com.example.spring_boot_project_api.service.MaintenanceCategoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/maintenance-categories")
@RequiredArgsConstructor
@Tag(name = "Maintenance Categories",
        description = "Maintenance category management endpoints (owner is resolved from the authenticated user)")
public class MaintenanceCategoryController {

    private final MaintenanceCategoryService categoryService;

    @Operation(summary = "Create a maintenance category",
            description = "Creates a maintenance category owned by the currently authenticated user. "
                    + "Name and code must be unique per owner.")
    @PostMapping
    public ResponseEntity<MaintenanceCategoryResponse> createCategory(
            @Valid @RequestBody CreateMaintenanceCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(request));
    }

    @Operation(summary = "List my maintenance categories",
            description = "Returns all maintenance categories belonging to the currently authenticated user.")
    @GetMapping
    public ResponseEntity<List<MaintenanceCategoryResponse>> getMyCategories() {
        return ResponseEntity.ok(categoryService.getMyCategories());
    }

    @Operation(summary = "Get a maintenance category by id",
            description = "Returns the maintenance category with the given id if it belongs to the currently authenticated user.")
    @GetMapping("/{id}")
    public ResponseEntity<MaintenanceCategoryResponse> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @Operation(summary = "Update a maintenance category",
            description = "Updates the maintenance category with the given id if it belongs to the currently authenticated user. "
                    + "Only provided fields are updated.")
    @PutMapping("/{id}")
    public ResponseEntity<MaintenanceCategoryResponse> updateCategory(@PathVariable Long id,
            @Valid @RequestBody UpdateMaintenanceCategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    @Operation(summary = "Deactivate a maintenance category",
            description = "Marks the maintenance category with the given id as INACTIVE "
                    + "if it belongs to the currently authenticated user.")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateCategory(@PathVariable Long id) {
        categoryService.deactivateCategory(id);
        return ResponseEntity.noContent().build();
    }
}
