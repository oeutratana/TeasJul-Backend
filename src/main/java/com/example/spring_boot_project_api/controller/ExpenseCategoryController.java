package com.example.spring_boot_project_api.controller;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.ExpenseCategoryRequest;
import com.example.spring_boot_project_api.dto.response.ExpenseCategoryResponse;
import com.example.spring_boot_project_api.service.ExpenseCategoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/expense-categories")
@RequiredArgsConstructor
public class ExpenseCategoryController {
    private final ExpenseCategoryService expenseCategoryService;

    @PostMapping
    public ResponseEntity<ExpenseCategoryResponse> createCategory(@Valid @RequestBody ExpenseCategoryRequest request) {
        ExpenseCategoryResponse response = expenseCategoryService.create(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpenseCategoryResponse> getCategoryById(
            @PathVariable Long id,
            @RequestParam Long ownerId) {
        return ResponseEntity.ok(expenseCategoryService.getByIdAndOwnerId(id, ownerId));
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<ExpenseCategoryResponse>> getAllByOwnerId(@PathVariable Long ownerId) {
        return ResponseEntity.ok(expenseCategoryService.getAllByOwnerId(ownerId));
    }

    @GetMapping("/owner/{ownerId}/paged")
    public ResponseEntity<Page<ExpenseCategoryResponse>> getAllByOwnerIdPaged(
            @PathVariable Long ownerId,
            Pageable pageable) {
        return ResponseEntity.ok(expenseCategoryService.getAllByOwnerId(ownerId, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenseCategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseCategoryRequest request) {
        return ResponseEntity.ok(expenseCategoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long id,
            @RequestParam Long ownerId) {
        expenseCategoryService.delete(id, ownerId);
        return ResponseEntity.noContent().build();
    }
}
