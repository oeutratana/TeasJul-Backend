package com.example.spring_boot_project_api.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.response.InvoiceSummaryResponse;
import com.example.spring_boot_project_api.enums.InvoiceStatus;
import com.example.spring_boot_project_api.enums.PaymentStatus;
import com.example.spring_boot_project_api.model.Invoice;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.repository.InvoiceRepository;
import com.example.spring_boot_project_api.repository.PaymentRepository;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.service.InvoiceSummaryService;
import com.example.spring_boot_project_api.util.PermissionConstants;
import com.example.spring_boot_project_api.util.SecurityUtils;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;

/**
 * Builds the invoice financial overview entirely from the existing
 * {@link InvoiceRepository} and {@link PaymentRepository} data. Collected money
 * always comes from actual PAID {@code Payment} records (reusing
 * {@link PaymentRepository#sumPaidAmountByInvoiceIds}), so expired or failed
 * payment attempts and partial payments are handled exactly like the rest of
 * the payment flow.
 */
@Service
@RequiredArgsConstructor
public class InvoiceSummaryServiceImpl implements InvoiceSummaryService {

    /** Invoices that count as "billed" when no explicit status filter is applied. */
    private static final List<InvoiceStatus> ISSUED_STATUSES =
            List.of(InvoiceStatus.PENDING, InvoiceStatus.PARTIAL,
                    InvoiceStatus.PAID, InvoiceStatus.OVERDUE);
    private static final List<InvoiceStatus> ALL_STATUSES =
            List.of(InvoiceStatus.values());
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final SecurityUtils securityUtils;
    private final HouseAccessService houseAccessService;

    @Override
    @Transactional(readOnly = true)
    public InvoiceSummaryResponse getSummary(Long houseId, Integer year, Integer month, InvoiceStatus status) {
        securityUtils.requirePermission(PermissionConstants.INVOICE_VIEW);

        List<Long> scopeHouseIds = resolveScopeHouseIds(houseId);
        int monthlyYear = resolveMonthlyYear(year);

        if (scopeHouseIds.isEmpty()) {
            return emptySummary(monthlyYear);
        }

        List<InvoiceStatus> scopeStatuses = status != null ? List.of(status) : ISSUED_STATUSES;

        List<Invoice> scopeInvoices = invoiceRepository.findAll(
                buildScopeSpec(scopeHouseIds, scopeStatuses, year, month));
        Map<Long, BigDecimal> collectedByInvoice = collectedByInvoice(scopeInvoices);

        BigDecimal totalInvoiced = sumNet(scopeInvoices);
        BigDecimal totalCollected = scopeInvoices.stream()
                .map(inv -> collected(inv, collectedByInvoice))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outstanding = scopeInvoices.stream()
                .map(inv -> remaining(inv, collectedByInvoice))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal overdue = scopeInvoices.stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.OVERDUE)
                .map(inv -> remaining(inv, collectedByInvoice))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal collectionRate = collectionRate(totalCollected, totalInvoiced);

        List<InvoiceSummaryResponse.StatusSummary> statusSummary =
                statusSummary(scopeHouseIds, year, month, status);
        List<InvoiceSummaryResponse.MonthlySeries> monthly =
                monthlySeries(scopeHouseIds, scopeStatuses, monthlyYear);

        return new InvoiceSummaryResponse(
                scopeInvoices.size(),
                totalInvoiced,
                totalCollected,
                outstanding,
                overdue,
                collectionRate,
                statusSummary,
                monthly,
                monthlyYear);
    }

    // ---- Scope ----------------------------------------------------------

    private List<Long> resolveScopeHouseIds(Long houseId) {
        if (houseId == null) {
            return houseAccessService.getAccessibleHouseIds();
        }
        // Throws AccessDeniedException when the current user has no right to this house.
        houseAccessService.requireHouseAccess(houseId);
        return List.of(houseId);
    }

    private int resolveMonthlyYear(Integer year) {
        if (year != null) {
            return year;
        }
        return LocalDate.now().getYear();
    }

    /**
     * Mirrors the house-scoping used by {@code InvoiceServiceImpl.getInvoices}:
     * invoice -> contract -> room -> house.
     */
    private Specification<Invoice> buildScopeSpec(List<Long> houseIds,
            List<InvoiceStatus> statuses, Integer year, Integer month) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Subquery<Long> roomSub = query.subquery(Long.class);
            Root<Room> roomRoot = roomSub.from(Room.class);
            roomSub.select(roomRoot.get("id"))
                    .where(roomRoot.get("houseId").in(houseIds));

            Subquery<Long> contractSub = query.subquery(Long.class);
            Root<RentalContract> contractRoot = contractSub.from(RentalContract.class);
            contractSub.select(contractRoot.get("id"))
                    .where(contractRoot.get("roomId").in(roomSub));

            predicates.add(root.get("contractId").in(contractSub));

            if (statuses != null && !statuses.isEmpty()) {
                predicates.add(root.get("status").in(statuses));
            }
            if (year != null) {
                predicates.add(cb.equal(root.get("billingYear"), year));
            }
            if (month != null) {
                predicates.add(cb.equal(root.get("billingMonth"), month));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // ---- Totals ---------------------------------------------------------

    private BigDecimal sumNet(List<Invoice> invoices) {
        return invoices.stream()
                .map(this::netAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** {@code max(totalAmount - discount, 0)} — the amount that was actually billed. */
    private BigDecimal netAmount(Invoice invoice) {
        BigDecimal total = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal discount = invoice.getDiscount() != null ? invoice.getDiscount() : BigDecimal.ZERO;
        return total.subtract(discount).max(BigDecimal.ZERO);
    }

    /** Remaining balance of an invoice after its actual collected amount. */
    private BigDecimal remaining(Invoice invoice, Map<Long, BigDecimal> collectedByInvoice) {
        return netAmount(invoice).subtract(collected(invoice, collectedByInvoice)).max(BigDecimal.ZERO);
    }

    private BigDecimal collected(Invoice invoice, Map<Long, BigDecimal> collectedByInvoice) {
        BigDecimal collected = invoice.getId() != null ? collectedByInvoice.get(invoice.getId()) : null;
        return collected != null ? collected : BigDecimal.ZERO;
    }

    /**
     * Actual collected money = sum of PAID payment records. Only the PAID status
     * is counted, and the repository already collapses the per-invoice amounts,
     * so EXPIRED / FAILED / PENDING attempts (multiple payment attempts) never
     * inflate the figure.
     */
    private Map<Long, BigDecimal> collectedByInvoice(List<Invoice> invoices) {
        if (invoices == null || invoices.isEmpty()) {
            return Map.of();
        }
        List<Long> invoiceIds = invoices.stream()
                .map(Invoice::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, BigDecimal> result = new HashMap<>();
        for (Object[] row : paymentRepository.sumPaidAmountByInvoiceIds(invoiceIds, PaymentStatus.PAID)) {
            if (row == null || row.length < 2 || row[0] == null || row[1] == null) {
                continue;
            }
            Long invoiceId = ((Number) row[0]).longValue();
            result.put(invoiceId, asBigDecimal(row[1]));
        }
        return result;
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal collectionRate(BigDecimal collected, BigDecimal invoiced) {
        if (invoiced == null || invoiced.signum() == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal collectedValue = collected != null ? collected : BigDecimal.ZERO;
        return collectedValue.multiply(HUNDRED)
                .divide(invoiced, 2, RoundingMode.HALF_UP);
    }

    // ---- Status breakdown ----------------------------------------------

    private List<InvoiceSummaryResponse.StatusSummary> statusSummary(List<Long> houseIds,
            Integer year, Integer month, InvoiceStatus filter) {
        Map<InvoiceStatus, Long> counts = new EnumMap<>(InvoiceStatus.class);
        Map<InvoiceStatus, BigDecimal> amounts = new EnumMap<>(InvoiceStatus.class);
        for (InvoiceStatus status : ALL_STATUSES) {
            counts.put(status, 0L);
            amounts.put(status, BigDecimal.ZERO);
        }

        List<Invoice> periodInvoices = invoiceRepository.findAll(
                buildScopeSpec(houseIds, ALL_STATUSES, year, month));
        for (Invoice invoice : periodInvoices) {
            if (invoice.getStatus() == null) {
                continue;
            }
            counts.merge(invoice.getStatus(), 1L, Long::sum);
            amounts.merge(invoice.getStatus(), netAmount(invoice), BigDecimal::add);
        }

        List<InvoiceSummaryResponse.StatusSummary> result = new ArrayList<>();
        for (InvoiceStatus status : ALL_STATUSES) {
            if (filter != null && filter != status) {
                continue;
            }
            result.add(new InvoiceSummaryResponse.StatusSummary(
                    status, counts.get(status), amounts.get(status)));
        }
        return result;
    }

    // ---- Monthly series -------------------------------------------------

    /**
     * Returns the 12 months of the given chart year. Each month is internally
     * consistent ({@code outstanding = invoiced - collected}), attributing
     * payments to the billing month of their invoice.
     */
    private List<InvoiceSummaryResponse.MonthlySeries> monthlySeries(
            List<Long> houseIds, List<InvoiceStatus> statuses, int year) {
        List<Invoice> yearInvoices = invoiceRepository.findAll(
                buildScopeSpec(houseIds, statuses, year, null));
        Map<Long, BigDecimal> collectedByInvoice = collectedByInvoice(yearInvoices);

        BigDecimal[] invoiced = new BigDecimal[12];
        BigDecimal[] collected = new BigDecimal[12];
        BigDecimal[] outstanding = new BigDecimal[12];
        for (int i = 0; i < 12; i++) {
            invoiced[i] = BigDecimal.ZERO;
            collected[i] = BigDecimal.ZERO;
            outstanding[i] = BigDecimal.ZERO;
        }

        for (Invoice invoice : yearInvoices) {
            int m = invoice.getBillingMonth() != null ? invoice.getBillingMonth() : 0;
            if (m < 1 || m > 12) {
                continue;
            }
            int idx = m - 1;
            BigDecimal net = netAmount(invoice);
            BigDecimal paid = collected(invoice, collectedByInvoice);
            invoiced[idx] = invoiced[idx].add(net);
            collected[idx] = collected[idx].add(paid);
            outstanding[idx] = outstanding[idx].add(net.subtract(paid).max(BigDecimal.ZERO));
        }

        List<InvoiceSummaryResponse.MonthlySeries> series = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            int idx = m - 1;
            series.add(new InvoiceSummaryResponse.MonthlySeries(
                    String.format("%04d-%02d", year, m),
                    invoiced[idx], collected[idx], outstanding[idx]));
        }
        return series;
    }

    // ---- Empty ----------------------------------------------------------

    private InvoiceSummaryResponse emptySummary(int monthlyYear) {
        List<InvoiceSummaryResponse.StatusSummary> statuses = new ArrayList<>();
        for (InvoiceStatus status : ALL_STATUSES) {
            statuses.add(new InvoiceSummaryResponse.StatusSummary(status, 0L, BigDecimal.ZERO));
        }
        List<InvoiceSummaryResponse.MonthlySeries> monthly = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            monthly.add(new InvoiceSummaryResponse.MonthlySeries(
                    String.format("%04d-%02d", monthlyYear, m),
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        }
        return new InvoiceSummaryResponse(0L, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                statuses, monthly, monthlyYear);
    }
}