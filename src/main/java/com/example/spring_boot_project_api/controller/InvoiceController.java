package com.example.spring_boot_project_api.controller;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.AddInvoiceItemRequest;
import com.example.spring_boot_project_api.dto.request.BulkGenerateInvoiceRequest;
import com.example.spring_boot_project_api.dto.request.GenerateInvoiceRequest;
import com.example.spring_boot_project_api.dto.request.UpdateInvoiceLateFeeRequest;
import com.example.spring_boot_project_api.dto.response.BulkGenerateInvoiceResponse;
import com.example.spring_boot_project_api.dto.response.InvoicePreviewResponse;
import com.example.spring_boot_project_api.dto.response.InvoiceResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.enums.InvoiceStatus;
import com.example.spring_boot_project_api.service.InvoiceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices",
        description = "Invoice generation, billing periods, invoice items, and payment status")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @Operation(summary = "Preview a monthly invoice",
            description = "Calculates rent + utility charges for a contract and billing period "
                    + "without persisting anything.")
    @PostMapping("/preview")
    public ResponseEntity<InvoicePreviewResponse> previewInvoice(
            @Valid @RequestBody GenerateInvoiceRequest request) {
        return ResponseEntity.ok(invoiceService.previewInvoice(request));
    }

    @Operation(summary = "Generate a monthly invoice",
            description = "Creates an invoice with a rent item and utility charges for the given "
                    + "contract and billing period. Usage is auto-calculated and rates are snapshotted."
                    + " Prevents duplicate invoices per contract per period.")
    @PostMapping("/generate")
    public ResponseEntity<InvoiceResponse> generateInvoice(
            @Valid @RequestBody GenerateInvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invoiceService.generateMonthlyInvoice(request));
    }

    @Operation(summary = "Bulk generate monthly invoices",
            description = "Creates invoices for ALL active contracts in one click for the given "
                    + "billing period. Contracts that already have an invoice for the period are skipped.")
    @PostMapping("/generate-bulk")
    public ResponseEntity<BulkGenerateInvoiceResponse> bulkGenerateInvoices(
            @Valid @RequestBody BulkGenerateInvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invoiceService.generateBulkInvoices(request));
    }

    @Operation(summary = "List invoices",
            description = "Returns invoices with filtering by house, search, status, billing period, "
                    + "tenant, and created-date range.")
    @GetMapping
    public ResponseEntity<PageResponse<InvoiceResponse>> getInvoices(
            @RequestParam(required = false) Long houseId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) Integer billingYear,
            @RequestParam(required = false) Integer billingMonth,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(invoiceService.getInvoices(houseId, search, status,
                billingYear, billingMonth, tenantId, fromDate, toDate, pageable));
    }

    @Operation(summary = "Get an invoice by id",
            description = "Returns the invoice (with its items) if it belongs to the current owner "
                    + "or is in a house the current manager has access to.")
    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getInvoiceById(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(id));
    }

    @Operation(summary = "Add an item to an invoice",
            description = "Adds a line item to a DRAFT or PENDING invoice. Amount is computed "
                    + "server-side as quantity x unit price.")
    @PostMapping("/{id}/items")
    public ResponseEntity<InvoiceResponse> addItem(@PathVariable Long id,
            @Valid @RequestBody AddInvoiceItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invoiceService.addItem(id, request));
    }

    @Operation(summary = "Update invoice status",
            description = "Manually sets the invoice status (e.g. PENDING, PARTIAL, PAID).")
    @PatchMapping("/{id}/status")
    public ResponseEntity<InvoiceResponse> updateStatus(@PathVariable Long id,
            @RequestParam InvoiceStatus status) {
        return ResponseEntity.ok(invoiceService.updateStatus(id, status));
    }

    @Operation(summary = "Mark invoice as paid",
            description = "Sets the invoice status to PAID.")
    @PatchMapping("/{id}/pay")
    public ResponseEntity<InvoiceResponse> markPaid(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.markPaid(id));
    }

    @Operation(summary = "Record a payment",
            description = "Records a partial or full payment and updates the invoice status "
                    + "(PARTIAL or PAID).")
    @PatchMapping("/{id}/payment")
    public ResponseEntity<InvoiceResponse> recordPayment(@PathVariable Long id,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(invoiceService.recordPayment(id, amount));
    }

    @Operation(summary = "Update the invoice late fee",
            description = "Stores an owner-supplied final late fee on an unpaid or partially paid "
                    + "invoice so the fee can be reduced or waived. Subtotal and discount are "
                    + "unchanged; the total due and remaining balance are recalculated. A fully "
                    + "paid invoice cannot be changed.")
    @PatchMapping("/{id}/late-fee")
    public ResponseEntity<InvoiceResponse> updateLateFee(@PathVariable Long id,
            @Valid @RequestBody UpdateInvoiceLateFeeRequest request) {
        return ResponseEntity.ok(invoiceService.updateLateFee(id, request.lateFee()));
    }

    @Operation(summary = "Recalculate the late fee",
            description = "Restores the policy late fee for the invoice due date. The amount is "
                    + "derived from the due date, so repeating the call does not increase the fee. "
                    + "A fully paid invoice is left unchanged.")
    @PatchMapping("/{id}/late-fee/recalculate")
    public ResponseEntity<InvoiceResponse> recalculateLateFee(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.recalculateLateFee(id));
    }

    @Operation(summary = "Cancel an invoice",
            description = "Marks the invoice as CANCELLED. A paid invoice cannot be cancelled.")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelInvoice(@PathVariable Long id) {
        invoiceService.cancelInvoice(id);
        return ResponseEntity.noContent().build();
    }
}
