package com.example.spring_boot_project_api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import com.example.spring_boot_project_api.dto.response.InvoiceSummaryResponse;
import com.example.spring_boot_project_api.enums.InvoiceStatus;
import com.example.spring_boot_project_api.enums.PaymentStatus;
import com.example.spring_boot_project_api.model.Invoice;
import com.example.spring_boot_project_api.repository.InvoiceRepository;
import com.example.spring_boot_project_api.repository.PaymentRepository;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
class InvoiceSummaryServiceImplTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private HouseAccessService houseAccessService;

    private InvoiceSummaryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InvoiceSummaryServiceImpl(
                invoiceRepository, paymentRepository, securityUtils, houseAccessService);
    }

    // 1. total invoiced
    @Test
    void totalInvoicedSumsTheBilledNetAmount() {
        stubInvoices(List.of(
                invoice(1L, 2026, 7, InvoiceStatus.PENDING, "200.00", "0.00"),
                invoice(2L, 2026, 7, InvoiceStatus.PAID, "100.00", "0.00")));
        stubCollectedRows();

        InvoiceSummaryResponse summary = service.getSummary(null, 2026, 7, null);

        assertThat(summary.totalInvoices()).isEqualTo(2);
        assertThat(summary.totalInvoiced()).isEqualByComparingTo("300.00");
        // DRAFT / CANCELLED are not part of the default "issued" scope.
        assertThat(summary.totalCollected()).isEqualByComparingTo("0.00");
    }

    @Test
    void totalInvoicedSubtractsTheDiscount() {
        stubInvoices(List.of(invoice(1L, 2026, 7, InvoiceStatus.PENDING, "250.00", "50.00")));
        stubCollectedRows();

        InvoiceSummaryResponse summary = service.getSummary(null, 2026, 7, null);

        assertThat(summary.totalInvoiced()).isEqualByComparingTo("200.00");
    }

    // 2. paid amount comes from actual PAID payments, via the payment repository
    @Test
    void collectedAmountComesFromPaidPayments() {
        stubInvoices(List.of(invoice(1L, 2026, 7, InvoiceStatus.PARTIAL, "200.00", "0.00")));
        stubCollectedRows(paidRow(1L, "120.00"));

        InvoiceSummaryResponse summary = service.getSummary(null, 2026, 7, null);

        assertThat(summary.totalCollected()).isEqualByComparingTo("120.00");
    }

    // 3. outstanding = invoiced - collected
    @Test
    void outstandingIsInvoicedMinusCollected() {
        stubInvoices(List.of(invoice(1L, 2026, 7, InvoiceStatus.PARTIAL, "200.00", "0.00")));
        stubCollectedRows(paidRow(1L, "120.00"));

        InvoiceSummaryResponse summary = service.getSummary(null, 2026, 7, null);

        assertThat(summary.totalInvoiced()).isEqualByComparingTo("200.00");
        assertThat(summary.totalCollected()).isEqualByComparingTo("120.00");
        assertThat(summary.outstanding()).isEqualByComparingTo("80.00");
    }

    // 4. overdue = remaining balance of OVERDUE invoices
    @Test
    void overdueUsesTheRemainingBalanceOfOverdueInvoices() {
        stubInvoices(List.of(
                invoice(1L, 2026, 7, InvoiceStatus.OVERDUE, "200.00", "0.00"),
                invoice(2L, 2026, 7, InvoiceStatus.PENDING, "100.00", "0.00")));
        stubCollectedRows(paidRow(1L, "50.00"));

        InvoiceSummaryResponse summary = service.getSummary(null, 2026, 7, null);

        assertThat(summary.overdue()).isEqualByComparingTo("150.00");
        // The pending-but-not-due invoice is not overdue.
        assertThat(summary.totalCollected()).isEqualByComparingTo("50.00");
    }

    // 5. collection rate = collected / invoiced * 100, rounded to 2 dp
    @Test
    void collectionRateIsCollectedDividedByInvoiced() {
        stubInvoices(List.of(invoice(1L, 2026, 7, InvoiceStatus.PARTIAL, "12845.50", "0.00")));
        stubCollectedRows(paidRow(1L, "10240.00"));

        InvoiceSummaryResponse summary = service.getSummary(null, 2026, 7, null);

        assertThat(summary.collectionRate()).isEqualByComparingTo("79.72");
    }

    // 6. monthly aggregation groups by billing month
    @Test
    void monthlyAggregationGroupsByBillingMonth() {
        stubInvoices(List.of(
                invoice(1L, 2026, 1, InvoiceStatus.PAID, "100.00", "0.00"),
                invoice(2L, 2026, 2, InvoiceStatus.PARTIAL, "200.00", "0.00"),
                invoice(3L, 2026, 3, InvoiceStatus.PENDING, "300.00", "0.00")));
        stubCollectedRows(paidRow(1L, "100.00"), paidRow(2L, "80.00"));

        InvoiceSummaryResponse summary = service.getSummary(null, 2026, null, null);

        assertThat(summary.monthly()).hasSize(12);
        assertThat(summary.monthlyYear()).isEqualTo(2026);
        assertThat(summary.monthly().get(0).month()).isEqualTo("2026-01");
        assertThat(summary.monthly().get(0).invoiced()).isEqualByComparingTo("100.00");
        assertThat(summary.monthly().get(0).collected()).isEqualByComparingTo("100.00");
        assertThat(summary.monthly().get(0).outstanding()).isEqualByComparingTo("0.00");
        assertThat(summary.monthly().get(1).invoiced()).isEqualByComparingTo("200.00");
        assertThat(summary.monthly().get(1).collected()).isEqualByComparingTo("80.00");
        assertThat(summary.monthly().get(1).outstanding()).isEqualByComparingTo("120.00");
        assertThat(summary.monthly().get(2).invoiced()).isEqualByComparingTo("300.00");
        assertThat(summary.monthly().get(9).invoiced()).isEqualByComparingTo("0.00");
    }

    // 8 + 9. multiple payment attempts: expired payment is never counted as collected
    @Test
    void expiredPaymentAttemptIsNotCountedAsCollected() {
        // The payment repository only sums PAID payments for an invoice, so the
        // EXPIRED attempt contributes 0. This mirrors:
        //   INV $121.37, Payment A $121.37 EXPIRED, Payment B $121.37 PAID.
        stubInvoices(List.of(invoice(1L, 2026, 7, InvoiceStatus.PAID, "121.37", "0.00")));
        stubCollectedRows(paidRow(1L, "121.37"));

        InvoiceSummaryResponse summary = service.getSummary(null, 2026, 7, null);

        assertThat(summary.totalInvoiced()).isEqualByComparingTo("121.37");
        assertThat(summary.totalCollected()).isEqualByComparingTo("121.37");
        assertThat(summary.outstanding()).isEqualByComparingTo("0.00");
    }

    @Test
    void onlyExpiredPaymentsMeanNothingWasCollected() {
        stubInvoices(List.of(
                invoice(1L, 2026, 7, InvoiceStatus.PENDING, "100.00", "0.00"),
                invoice(2L, 2026, 7, InvoiceStatus.PARTIAL, "50.00", "0.00")));
        // Expired/failed attempts exist in the DB but the PAID aggregate is 0.
        stubCollectedRows();

        InvoiceSummaryResponse summary = service.getSummary(null, 2026, 7, null);

        assertThat(summary.totalCollected()).isEqualByComparingTo("0.00");
        assertThat(summary.outstanding()).isEqualByComparingTo("150.00");
    }

    // 10. partial payment
    @Test
    void partialPaymentComputesCollectedAndOutstanding() {
        stubInvoices(List.of(invoice(1L, 2026, 7, InvoiceStatus.PARTIAL, "200.00", "0.00")));
        stubCollectedRows(paidRow(1L, "120.00"));

        InvoiceSummaryResponse summary = service.getSummary(null, 2026, 7, null);

        assertThat(summary.totalInvoiced()).isEqualByComparingTo("200.00");
        assertThat(summary.totalCollected()).isEqualByComparingTo("120.00");
        assertThat(summary.outstanding()).isEqualByComparingTo("80.00");
    }

    // 11. zero invoices never divides by zero
    @Test
    void zeroInvoicesDoesNotDivideByZero() {
        stubInvoices(List.of());
        stubCollectedRows();

        InvoiceSummaryResponse summary = service.getSummary(null, 2026, 7, null);

        assertThat(summary.totalInvoices()).isZero();
        assertThat(summary.totalInvoiced()).isEqualByComparingTo("0.00");
        assertThat(summary.totalCollected()).isEqualByComparingTo("0.00");
        assertThat(summary.outstanding()).isEqualByComparingTo("0.00");
        assertThat(summary.overdue()).isEqualByComparingTo("0.00");
        assertThat(summary.collectionRate()).isEqualByComparingTo("0.00");
        assertThat(summary.monthly()).hasSize(12);
    }

    // 12. filters
    @Test
    void statusFilterRestrictsTheStatusBreakdown() {
        stubInvoices(List.of(invoice(1L, 2026, 8, InvoiceStatus.PENDING, "150.00", "0.00")));
        stubCollectedRows();

        InvoiceSummaryResponse summary = service.getSummary(null, 2026, null, InvoiceStatus.PENDING);

        assertThat(summary.totalInvoices()).isEqualTo(1);
        assertThat(summary.statusSummary())
                .extracting(InvoiceSummaryResponse.StatusSummary::status)
                .containsExactly(InvoiceStatus.PENDING);
        assertThat(summary.statusSummary().get(0).amount()).isEqualByComparingTo("150.00");
    }

    @Test
    void monthFilterScopesStatsButKeepsTheFullYearChart() {
        stubInvoices(List.of(invoice(1L, 2026, 7, InvoiceStatus.PAID, "400.00", "0.00")));
        stubCollectedRows(paidRow(1L, "400.00"));

        InvoiceSummaryResponse summary = service.getSummary(null, 2026, 7, null);

        assertThat(summary.totalInvoiced()).isEqualByComparingTo("400.00");
        // The chart always covers the selected year (12 months), not just the month.
        assertThat(summary.monthlyYear()).isEqualTo(2026);
        assertThat(summary.monthly()).hasSize(12);
        assertThat(summary.monthly().get(6).month()).isEqualTo("2026-07");
    }

    @Test
    void defaultChartYearIsTheCurrentYear() {
        stubInvoices(List.of());
        stubCollectedRows();

        InvoiceSummaryResponse summary = service.getSummary(null, null, null, null);

        assertThat(summary.monthlyYear()).isEqualTo(java.time.LocalDate.now().getYear());
        assertThat(summary.monthly()).hasSize(12);
    }

    // Status summary contains every supported status with count + amount
    @Test
    void statusSummaryContainsAllSupportedStatuses() {
        stubInvoices(List.of(
                invoice(1L, 2026, 7, InvoiceStatus.PAID, "1000.00", "0.00"),
                invoice(2L, 2026, 7, InvoiceStatus.PENDING, "500.00", "0.00"),
                invoice(3L, 2026, 7, InvoiceStatus.CANCELLED, "200.00", "0.00")));
        stubCollectedRows(paidRow(1L, "1000.00"));

        InvoiceSummaryResponse summary = service.getSummary(null, 2026, 7, null);

        Map<InvoiceStatus, InvoiceSummaryResponse.StatusSummary> byStatus = summary.statusSummary()
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        InvoiceSummaryResponse.StatusSummary::status, s -> s));
        assertThat(summary.statusSummary()).hasSize(InvoiceStatus.values().length);
        assertThat(byStatus.get(InvoiceStatus.PAID).count()).isEqualTo(1);
        assertThat(byStatus.get(InvoiceStatus.PAID).amount()).isEqualByComparingTo("1000.00");
        assertThat(byStatus.get(InvoiceStatus.PENDING).amount()).isEqualByComparingTo("500.00");
        assertThat(byStatus.get(InvoiceStatus.DRAFT).count()).isZero();
    }

    // 7. property isolation — house access is enforced before any query runs
    @Test
    void unauthorizedHouseIsRejected() {
        doThrow(new AccessDeniedException("You do not have permission to access this resource"))
                .when(houseAccessService).requireHouseAccess(99L);

        assertThatThrownBy(() -> service.getSummary(99L, 2026, 7, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void noAccessibleHousesReturnsAnEmptySummaryWithoutQuerying() {
        when(houseAccessService.getAccessibleHouseIds()).thenReturn(List.of());

        InvoiceSummaryResponse summary = service.getSummary(null, 2026, null, null);

        assertThat(summary.totalInvoices()).isZero();
        assertThat(summary.totalInvoiced()).isEqualByComparingTo("0.00");
        assertThat(summary.collectionRate()).isEqualByComparingTo("0.00");
        assertThat(summary.statusSummary()).hasSize(InvoiceStatus.values().length);
        assertThat(summary.monthly()).hasSize(12);
    }

    // ---- helpers ----

    private void stubInvoices(List<Invoice> invoices) {
        lenient().when(houseAccessService.getAccessibleHouseIds()).thenReturn(List.of(1L));
        lenient().when(invoiceRepository.findAll(any(Specification.class))).thenReturn(invoices);
    }

    private void stubCollectedRows(Object[]... rows) {
        lenient().when(paymentRepository.sumPaidAmountByInvoiceIds(anyList(), eq(PaymentStatus.PAID)))
                .thenReturn(java.util.Arrays.asList(rows));
    }

    private Object[] paidRow(Long invoiceId, String amount) {
        return new Object[] { invoiceId, new BigDecimal(amount) };
    }

    private Invoice invoice(Long id, int year, int month, InvoiceStatus status,
            String total, String discount) {
        return Invoice.builder()
                .id(id)
                .contractId(100L + id)
                .billingYear(year)
                .billingMonth(month)
                .status(status)
                .totalAmount(new BigDecimal(total))
                .discount(new BigDecimal(discount))
                .build();
    }
}