package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.CreateRenewalRequest;
import com.example.spring_boot_project_api.dto.response.ContractRenewalResponse;
import com.example.spring_boot_project_api.service.ContractRenewalService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/contract_renewals")
@RequiredArgsConstructor
public class ContractRenewalController {
    private final ContractRenewalService renewalService;

    @PostMapping
    public ResponseEntity<ContractRenewalResponse> createRenewal(
            @Valid @RequestBody CreateRenewalRequest request) {
        ContractRenewalResponse response = renewalService.createRenewal(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContractRenewalResponse> getRenewalById(@PathVariable Long id) {
        return ResponseEntity.ok(renewalService.getRenewalById(id));
    }

    @GetMapping("/contract/{contractId}")
    public ResponseEntity<List<ContractRenewalResponse>> getRenewalsByContract(
            @PathVariable Long contractId) {
        return ResponseEntity.ok(renewalService.getRenewalsByContract(contractId));
    }
}
