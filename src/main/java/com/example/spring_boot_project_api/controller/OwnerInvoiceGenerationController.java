package com.example.spring_boot_project_api.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.response.ContractEligibleResponse;
import com.example.spring_boot_project_api.dto.response.GenerateBulkPreviewResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.service.InvoiceService;
import com.example.spring_boot_project_api.util.PermissionConstants;
import com.example.spring_boot_project_api.util.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/owner/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoice Generation",
        description = "Eligible contracts and bulk-generation preview counts for the "
                + "authenticated user's accessible houses")
public class OwnerInvoiceGenerationController {

    private final InvoiceService invoiceService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "List eligible contracts for invoice creation",
            description = "Returns ACTIVE contracts in the requested (accessible) house or all "
                    + "accessible houses, optionally filtered by contract number, room number or "
                    + "tenant name. The alreadyInvoiced flag is set for the given billing period.")
    @GetMapping("/eligible-contracts")
    public PageResponse<ContractEligibleResponse> getEligibleContracts(
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer billingYear,
            @RequestParam(required = false) Integer billingMonth,
            @PageableDefault(size = 10, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        securityUtils.requirePermission(PermissionConstants.INVOICE_CREATE);
        return invoiceService.getEligibleContracts(propertyId, search, billingYear, billingMonth, pageable);
    }

    @Operation(summary = "Preview bulk invoice generation",
            description = "Returns how many ACTIVE contracts exist in the selected house scope, how "
                    + "many already have an invoice for the period and how many would be generated.")
    @GetMapping("/generate-preview")
    public GenerateBulkPreviewResponse previewBulkGeneration(
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) Integer billingYear,
            @RequestParam(required = false) Integer billingMonth) {
        securityUtils.requirePermission(PermissionConstants.INVOICE_CREATE);
        return invoiceService.previewBulkGeneration(propertyId, billingYear, billingMonth);
    }
}