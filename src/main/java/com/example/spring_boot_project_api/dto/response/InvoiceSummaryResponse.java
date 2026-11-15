package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.example.spring_boot_project_api.enums.InvoiceStatus;

/**
 * Aggregate financial overview of invoices, scoped to the authenticated user's
 * accessible houses and the requested filters.
 *
 * <p>Financial definitions (consistent with the project's existing invoice /
 * payment logic):
 * <ul>
 *   <li>{@code totalInvoiced} — sum of {@code totalAmount - discount} over the
 *       scoped invoices. Without an explicit status filter this excludes DRAFT
 *       and CANCELLED invoices (only issued invoices are counted as billed).</li>
 *   <li>{@code totalCollected} — sum of the actual PAID {@link com.example.spring_boot_project_api.model.Payment}
 *       amounts for the scoped invoices. EXPIRED / FAILED / PENDING / CANCELLED
 *       payment attempts are never counted, so multiple payment attempts do not
 *       double count money.</li>
 *   <li>{@code outstanding} — sum of {@code max(invoiced - collected, 0)} per
 *       invoice (handles partial payments correctly).</li>
 *   <li>{@code overdue} — sum of the remaining balance of invoices whose status
 *       is {@link InvoiceStatus#OVERDUE}.</li>
 *   <li>{@code collectionRate} — {@code collected / invoiced * 100} (0 when there
 *       is nothing invoiced).</li>
 * </ul>
 */
public record InvoiceSummaryResponse(
        long totalInvoices,
        BigDecimal totalInvoiced,
        BigDecimal totalCollected,
        BigDecimal outstanding,
        BigDecimal overdue,
        BigDecimal collectionRate,
        List<StatusSummary> statusSummary,
        List<MonthlySeries> monthly,
        int monthlyYear
) {

    public record StatusSummary(
            InvoiceStatus status,
            long count,
            BigDecimal amount
    ) {
    }

    public record MonthlySeries(
            String month,
            BigDecimal invoiced,
            BigDecimal collected,
            BigDecimal outstanding
    ) {
    }
}