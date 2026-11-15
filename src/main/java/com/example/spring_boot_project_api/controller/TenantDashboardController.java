package com.example.spring_boot_project_api.controller;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.TenantDashboardResponse;
import com.example.spring_boot_project_api.dto.response.TenantInvoiceDetailResponse;
import com.example.spring_boot_project_api.dto.response.TenantInvoiceResponse;
import com.example.spring_boot_project_api.dto.response.TenantPaymentHistoryResponse;
import com.example.spring_boot_project_api.dto.response.TenantUtilityTrendResponse;
import com.example.spring_boot_project_api.exception.InvalidTenantQueryException;
import com.example.spring_boot_project_api.service.TenantDashboardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tenant")
@RequiredArgsConstructor
public class TenantDashboardController {

    private final TenantDashboardService tenantDashboardService;

    @GetMapping("/dashboard")
    public TenantDashboardResponse getDashboard() {
        return tenantDashboardService.getDashboard();
    }

    @GetMapping("/utilities/usage")
    public TenantUtilityTrendResponse getUtilityUsage(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return tenantDashboardService.getUtilityUsage(parseMonth(from, "from"), parseMonth(to, "to"));
    }

    @GetMapping("/payments/history")
    public List<TenantPaymentHistoryResponse> getPaymentHistory(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return tenantDashboardService.getPaymentHistory(parseMonth(from, "from"), parseMonth(to, "to"));
    }

    @GetMapping("/invoices")
    public PageResponse<TenantInvoiceResponse> getMyInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return tenantDashboardService.getMyInvoices(page, size);
    }

    @GetMapping("/invoices/{invoiceId}")
    public TenantInvoiceDetailResponse getMyInvoice(@PathVariable Long invoiceId) {
        return tenantDashboardService.getMyInvoice(invoiceId);
    }

    private YearMonth parseMonth(String value, String param) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (!value.matches("\\d{4}-\\d{2}")) {
            throw new InvalidTenantQueryException(param + " must be a valid month in yyyy-MM format");
        }
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException ex) {
            throw new InvalidTenantQueryException(param + " must be a valid month in yyyy-MM format");
        }
    }
}