package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.ContractTenantRequest;
import com.example.spring_boot_project_api.dto.response.ContractTenantResponse;
import com.example.spring_boot_project_api.service.ContractTenantService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/contract_tenant")
@RequiredArgsConstructor
public class ContractTenantController {
    private final ContractTenantService contractTenantService;

    @PostMapping
    public ResponseEntity<ContractTenantResponse> addTenantToContract(@Valid @RequestBody ContractTenantRequest request) {
        ContractTenantResponse response = contractTenantService.addTenantToContract(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContractTenantResponse> getContractTenantById(@PathVariable Long id) {
        return ResponseEntity.ok(contractTenantService.getContractTenantById(id));
    }

    @GetMapping("/contract/{contractId}")
    public ResponseEntity<List<ContractTenantResponse>> getTenantsByContract(@PathVariable Long contractId) {
        return ResponseEntity.ok(contractTenantService.getTenantsByContract(contractId));
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<ContractTenantResponse>> getContractsByTenant(@PathVariable Long tenantId) {
        return ResponseEntity.ok(contractTenantService.getContractsByTenant(tenantId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContractTenantResponse> updateContractTenant(
            @PathVariable Long id,
            @Valid @RequestBody ContractTenantRequest request) {
        return ResponseEntity.ok(contractTenantService.updateContractTenant(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeTenantFromContract(@PathVariable Long id) {
        contractTenantService.removeTenantFromContract(id);
        return ResponseEntity.noContent().build();
    }
}
