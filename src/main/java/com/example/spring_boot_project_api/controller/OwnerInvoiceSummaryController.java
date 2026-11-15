package com.example.spring_boot_project_api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.response.InvoiceSummaryResponse;
import com.example.spring_boot_project_api.enums.InvoiceStatus;
import com.example.spring_boot_project_api.service.InvoiceSummaryService;
import com.example.spring_boot_project_api.util.PermissionConstants;
import com.example.spring_boot_project_api.util.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/owner/invoices")
@RequiredArgsConstructor
@Tag(name = "Owner Invoice Summary",
        description = "Financial summary, status breakdown and monthly overview for the "
                + "owner's invoices, scoped to the authenticated user's accessible houses")
public class OwnerInvoiceSummaryController {

    private final InvoiceSummaryService invoiceSummaryService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "Get invoice financial summary",
            description = "Returns total invoiced, actual collected, outstanding, overdue, "
                    + "collection rate, per-status breakdown and a monthly series. Filters are "
                    + "optional; the monthly chart is driven by the selected year (defaults to "
                    + "the current year) while the month filter only narrows the headline stats.")
    @GetMapping("/summary")
    public InvoiceSummaryResponse getSummary(
            @RequestParam(required = false) Long houseId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) InvoiceStatus status) {
        securityUtils.requirePermission(PermissionConstants.INVOICE_VIEW);
        return invoiceSummaryService.getSummary(houseId, year, month, status);
    }
}