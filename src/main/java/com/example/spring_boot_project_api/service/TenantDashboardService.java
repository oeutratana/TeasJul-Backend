package com.example.spring_boot_project_api.service;

import java.time.YearMonth;
import java.util.List;

import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.TenantDashboardResponse;
import com.example.spring_boot_project_api.dto.response.TenantInvoiceDetailResponse;
import com.example.spring_boot_project_api.dto.response.TenantInvoiceResponse;
import com.example.spring_boot_project_api.dto.response.TenantPaymentHistoryResponse;
import com.example.spring_boot_project_api.dto.response.TenantUtilityTrendResponse;

/**
 * Read-only dashboard data for the currently authenticated tenant.
 *
 * <p>Every method resolves the tenant from the authenticated user (never from the
 * client request) and only ever returns data belonging to that tenant — a
 * tenant can never observe another tenant's invoices, payments or utility data.
 */
public interface TenantDashboardService {

    TenantDashboardResponse getDashboard();

    /**
     * Monthly utility consumption trend for the tenant's current room. When
     * {@code from}/{@code to} are {@code null} the last 6 months (inclusive) are used.
     */
    TenantUtilityTrendResponse getUtilityUsage(YearMonth from, YearMonth to);

    /**
     * Per-billing-month payment summary. When {@code from}/{@code to} are
     * {@code null} the last 6 months (inclusive) are used.
     */
    List<TenantPaymentHistoryResponse> getPaymentHistory(YearMonth from, YearMonth to);

    PageResponse<TenantInvoiceResponse> getMyInvoices(int page, int size);

    TenantInvoiceDetailResponse getMyInvoice(Long invoiceId);
}