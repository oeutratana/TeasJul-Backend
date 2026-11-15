package com.example.spring_boot_project_api.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.domain.Pageable;

import com.example.spring_boot_project_api.dto.request.AddInvoiceItemRequest;
import com.example.spring_boot_project_api.dto.request.BulkGenerateInvoiceRequest;
import com.example.spring_boot_project_api.dto.request.GenerateInvoiceRequest;
import com.example.spring_boot_project_api.dto.response.BulkGenerateInvoiceResponse;
import com.example.spring_boot_project_api.dto.response.ContractEligibleResponse;
import com.example.spring_boot_project_api.dto.response.GenerateBulkPreviewResponse;
import com.example.spring_boot_project_api.dto.response.InvoicePreviewResponse;
import com.example.spring_boot_project_api.dto.response.InvoiceResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.enums.InvoiceStatus;

public interface InvoiceService {

    InvoiceResponse generateMonthlyInvoice(GenerateInvoiceRequest request);

    BulkGenerateInvoiceResponse generateBulkInvoices(BulkGenerateInvoiceRequest request);

    InvoicePreviewResponse previewInvoice(GenerateInvoiceRequest request);

    PageResponse<ContractEligibleResponse> getEligibleContracts(Long houseId, String search,
            Integer billingYear, Integer billingMonth, Pageable pageable);

    GenerateBulkPreviewResponse previewBulkGeneration(Long houseId, Integer billingYear, Integer billingMonth);

    PageResponse<InvoiceResponse> getInvoices(Long houseId, String search, InvoiceStatus status,
            Integer billingYear, Integer billingMonth, Long tenantId, LocalDate fromDate,
            LocalDate toDate, Pageable pageable);

    InvoiceResponse getInvoiceById(Long id);

    InvoiceResponse addItem(Long id, AddInvoiceItemRequest request);

    InvoiceResponse updateStatus(Long id, InvoiceStatus status);

    void cancelInvoice(Long id);

    InvoiceResponse markPaid(Long id);

    InvoiceResponse recordPayment(Long id, BigDecimal paidAmount);

    /** Stores an owner-supplied final late fee (reduce or waive) on an unpaid invoice. */
    InvoiceResponse updateLateFee(Long id, BigDecimal lateFee);

    /** Restores the policy late fee for the invoice's due date without changing subtotal or discount. */
    InvoiceResponse recalculateLateFee(Long id);
}
