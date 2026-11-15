package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.RentalContractRequest;
import com.example.spring_boot_project_api.dto.response.ContractSummaryResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.RentalContractResponse;
import com.example.spring_boot_project_api.enums.RentalContractStatus;
import com.example.spring_boot_project_api.service.RentalContractService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/rental-contracts")
public class RentalContractController {
    private final RentalContractService contractService;

    public RentalContractController(RentalContractService contractService) {
        this.contractService = contractService;
    }

    @PostMapping
    public ResponseEntity<RentalContractResponse> createContract(@Valid @RequestBody RentalContractRequest request) {
        RentalContractResponse createdContract = contractService.createContract(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdContract);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RentalContractResponse> getContractById(@PathVariable Long id) {
        return ResponseEntity.ok(contractService.getContractById(id));
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<PageResponse<RentalContractResponse>> getContractsByOwner(
            @PathVariable Long ownerId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) RentalContractStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(contractService.getContractsByOwner(ownerId, search, status, pageable));
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<RentalContractResponse>> getContractsByRoom(@PathVariable Long roomId) {
        return ResponseEntity.ok(contractService.getContractsByRoom(roomId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<PageResponse<RentalContractResponse>> getContractsByStatus(
            @PathVariable RentalContractStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(contractService.getContractsByStatus(status, pageable));
    }

    @GetMapping("/summary")
    public ResponseEntity<ContractSummaryResponse> getContractsSummary() {
        return ResponseEntity.ok(contractService.getContractsSummary());
    }

    @PutMapping("/{id}")
    public ResponseEntity<RentalContractResponse> updateContract(
            @PathVariable Long id,
            @Valid @RequestBody RentalContractRequest request) {
        return ResponseEntity.ok(contractService.updateContract(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<RentalContractResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam RentalContractStatus status) {
        return ResponseEntity.ok(contractService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContract(@PathVariable Long id) {
        contractService.deleteContract(id);
        return ResponseEntity.noContent().build();
    }
}
