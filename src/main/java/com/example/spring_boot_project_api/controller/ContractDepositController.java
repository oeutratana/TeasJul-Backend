package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.CreateDepositRequest;
import com.example.spring_boot_project_api.dto.request.UpdateDepositPaymentRequest;
import com.example.spring_boot_project_api.dto.response.ContractDepositResponse;
import com.example.spring_boot_project_api.service.ContractDepositService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/contract_deposit")
@RequiredArgsConstructor
@Tag(name = "Contract Deposits", description = "Contract deposit management endpoints")
public class ContractDepositController {
    private final ContractDepositService depositService;

    @Operation(summary = "Create a new deposit")
    @PostMapping
    public ResponseEntity<ContractDepositResponse> createDeposit(
            @Valid @RequestBody CreateDepositRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(depositService.createDeposit(request));
    }

    @Operation(summary = "Mark a deposit as paid")
    @PatchMapping("/{id}/pay")
    public ResponseEntity<ContractDepositResponse> markAsPaid(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDepositPaymentRequest request) {
        return ResponseEntity.ok(depositService.markAsPaid(id, request));
    }

    @Operation(summary = "Get a deposit by id")
    @GetMapping("/{id}")
    public ResponseEntity<ContractDepositResponse> getDepositById(@PathVariable Long id) {
        return ResponseEntity.ok(depositService.getDepositById(id));
    }

    @Operation(summary = "Get all deposits for a contract")
    @GetMapping("/contract/{contractId}")
    public ResponseEntity<List<ContractDepositResponse>> getDepositsByContract(
            @PathVariable Long contractId) {
        return ResponseEntity.ok(depositService.getDepositsByContract(contractId));
    }
}
