package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import com.example.spring_boot_project_api.dto.request.ExpenseRequest;
import com.example.spring_boot_project_api.dto.response.ExpenseResponse;
import com.example.spring_boot_project_api.service.ExpenseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
public class ExpenseController {
    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(@Valid @RequestBody ExpenseRequest request) {
        ExpenseResponse response = expenseService.create(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpenseResponse> getExpenseById(
            @PathVariable Long id,
            @RequestParam Long ownerId) {
        return ResponseEntity.ok(expenseService.getByIdAndOwnerId(id, ownerId));
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<ExpenseResponse>> getAllByOwnerId(@PathVariable Long ownerId) {
        return ResponseEntity.ok(expenseService.getAllByOwnerId(ownerId));
    }

    @GetMapping("/owner/{ownerId}/paged")
    public ResponseEntity<Page<ExpenseResponse>> getAllByOwnerIdPaged(
            @PathVariable Long ownerId,
            Pageable pageable) {
        return ResponseEntity.ok(expenseService.getAllByOwnerId(ownerId, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenseResponse> updateExpense(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.ok(expenseService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(
            @PathVariable Long id,
            @RequestParam Long ownerId) {
        expenseService.delete(id, ownerId);
        return ResponseEntity.noContent().build();
    }
}
