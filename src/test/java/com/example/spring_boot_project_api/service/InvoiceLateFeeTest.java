package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.example.spring_boot_project_api.dto.request.UpdateInvoiceLateFeeRequest;
import com.example.spring_boot_project_api.dto.response.InvoiceResponse;
import com.example.spring_boot_project_api.enums.InvoiceItemType;
import com.example.spring_boot_project_api.enums.InvoiceStatus;
import com.example.spring_boot_project_api.exception.InvalidInvoiceOperationException;
import com.example.spring_boot_project_api.mapper.InvoiceMapper;
import com.example.spring_boot_project_api.model.Invoice;
import com.example.spring_boot_project_api.model.InvoiceItem;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.ContractTenantRepository;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.InvoiceRepository;
import com.example.spring_boot_project_api.repository.PaymentRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.RoomUtilityRepository;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.UtilityMeterRepository;
import com.example.spring_boot_project_api.repository.UtilityRateRepository;
import com.example.spring_boot_project_api.repository.UtilityTypeRepository;
import com.example.spring_boot_project_api.service.impl.InvoiceServiceImpl;
import com.example.spring_boot_project_api.util.InvoiceBalanceCalculator;
import com.example.spring_boot_project_api.util.SecurityUtils;

/**
 * Focused coverage for the stored late-fee policy. Date arithmetic is asserted with
 * literal fixed dates; service-level tests derive their due dates from a single captured
 * "today" so they never depend on a particular real-world day.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InvoiceLateFeeTest {

    private static final LocalDate FIXED_TODAY = LocalDate.of(2026, 9, 27);

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private InvoiceMapper invoiceMapper;
    @Mock private RentalContractRepository contractRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private HouseRepository houseRepository;
    @Mock private ContractTenantRepository contractTenantRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private UtilityMeterRepository meterRepository;
    @Mock private UtilityRateRepository rateRepository;
    @Mock private UtilityTypeRepository typeRepository;
    @Mock private RoomUtilityRepository roomUtilityRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserHouseAccessRepository userHouseAccessRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private NotificationService notificationService;
    @Mock private OwnerSettingService ownerSettingService;
    @Mock private HouseAccessService houseAccessService;

    @InjectMocks private InvoiceServiceImpl invoiceService;

    /** Captured once per test so day offsets are stable even across a midnight rollover. */
    private LocalDate today;

    private User owner;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
        owner = User.builder().id(1L).username("owner").email("owner@example.com")
                .passwordHash("h").firstName("Owner").lastName("One").build();
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(paymentRepository.findPaidTotalByInvoiceId(any())).thenReturn(BigDecimal.ZERO);
        when(contractRepository.findById(any())).thenReturn(Optional.empty());
    }

    // ---- 1-6, 14, 15: pure policy arithmetic on fixed dates ------------

    @Nested
    @DisplayName("policy arithmetic on fixed dates")
    class PolicyArithmetic {

        @Test
        @DisplayName("1. due today -> 0.00")
        void dueTodayIsFree() {
            assertThat(InvoiceBalanceCalculator.overdueDays(FIXED_TODAY, FIXED_TODAY)).isZero();
            assertThat(InvoiceBalanceCalculator.defaultLateFee(FIXED_TODAY, FIXED_TODAY))
                    .isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("2. one day overdue -> 1.00")
        void oneDayOverdue() {
            LocalDate due = FIXED_TODAY.minusDays(1);
            assertThat(InvoiceBalanceCalculator.overdueDays(due, FIXED_TODAY)).isEqualTo(1L);
            assertThat(InvoiceBalanceCalculator.defaultLateFee(due, FIXED_TODAY))
                    .isEqualByComparingTo("1.00");
        }

        @Test
        @DisplayName("3. five days overdue -> 5.00")
        void fiveDaysOverdue() {
            LocalDate due = FIXED_TODAY.minusDays(5);
            assertThat(InvoiceBalanceCalculator.defaultLateFee(due, FIXED_TODAY))
                    .isEqualByComparingTo("5.00");
        }

        @Test
        @DisplayName("4. twenty-two days overdue (Sep 5 -> Sep 27) -> 22.00")
        void twentyTwoDaysOverdue() {
            LocalDate due = LocalDate.of(2026, 9, 5);
            assertThat(InvoiceBalanceCalculator.overdueDays(due, FIXED_TODAY)).isEqualTo(22L);
            assertThat(InvoiceBalanceCalculator.defaultLateFee(due, FIXED_TODAY))
                    .isEqualByComparingTo("22.00");
        }

        @Test
        @DisplayName("5. future due date -> 0.00 and never negative")
        void futureDueDateIsFree() {
            LocalDate due = FIXED_TODAY.plusDays(10);
            assertThat(InvoiceBalanceCalculator.overdueDays(due, FIXED_TODAY)).isZero();
            assertThat(InvoiceBalanceCalculator.defaultLateFee(due, FIXED_TODAY))
                    .isEqualByComparingTo("0.00");
            assertThat(InvoiceBalanceCalculator.defaultLateFee(due, FIXED_TODAY).signum()).isNotNegative();
        }

        @Test
        @DisplayName("6. recalculating on the same day is idempotent")
        void sameDayRecalculationIsIdempotent() {
            LocalDate due = LocalDate.of(2026, 9, 5);
            BigDecimal first = InvoiceBalanceCalculator.defaultLateFee(due, FIXED_TODAY);
            BigDecimal second = InvoiceBalanceCalculator.defaultLateFee(due, FIXED_TODAY);
            BigDecimal third = InvoiceBalanceCalculator.defaultLateFee(due, FIXED_TODAY);
            assertThat(first).isEqualByComparingTo("22.00");
            assertThat(second).isEqualByComparingTo(first);
            assertThat(third).isEqualByComparingTo(first);
            // Deriving from the due date (not the stored fee) is what prevents 22 -> 23 -> 24.
            assertThat(InvoiceBalanceCalculator.defaultLateFee(due, FIXED_TODAY.plusDays(1)))
                    .isEqualByComparingTo("23.00");
        }

        @Test
        @DisplayName("14. totalDue = subtotal - discount + lateFee")
        void totalDueFormula() {
            assertThat(InvoiceBalanceCalculator.invoiceTotal(new BigDecimal("159.52"),
                    new BigDecimal("0.00"), new BigDecimal("22.00")))
                    .isEqualByComparingTo("181.52");
            assertThat(InvoiceBalanceCalculator.invoiceTotal(new BigDecimal("200.00"),
                    new BigDecimal("20.00"), new BigDecimal("5.00")))
                    .isEqualByComparingTo("185.00");
        }

        @Test
        @DisplayName("15. remaining = max(totalDue - paid, 0) and PAID stays at zero")
        void remainingFormula() {
            assertThat(InvoiceBalanceCalculator.remainingBalance(new BigDecimal("181.52"),
                    new BigDecimal("50.00"))).isEqualByComparingTo("131.52");
            assertThat(InvoiceBalanceCalculator.remainingBalance(new BigDecimal("181.52"),
                    new BigDecimal("181.52"))).isEqualByComparingTo("0.00");
            // Overpayment can never produce a negative remaining balance.
            assertThat(InvoiceBalanceCalculator.remainingBalance(new BigDecimal("181.52"),
                    new BigDecimal("200.00"))).isEqualByComparingTo("0.00");
        }
    }

    // ---- 7-13: service behaviour --------------------------------------

    @Nested
    @DisplayName("service behaviour")
    class ServiceBehaviour {

        @Test
        @DisplayName("7/8. overdue recalculation stores the fee and returns it in the response")
        void recalculationPersistsFeeAndExposesIt() {
            Invoice invoice = invoiceWithSubtotal("159.52", "0.00", today.minusDays(22), InvoiceStatus.PENDING);
            stubAccessible(invoice);

            InvoiceResponse response = invoiceService.recalculateLateFee(invoice.getId());

            Invoice saved = captureSaved();
            assertThat(saved.getLateFee()).isEqualByComparingTo("22.00");
            assertThat(saved.getTotalAmount()).isEqualByComparingTo("181.52");
            assertThat(response.lateFee()).isEqualByComparingTo("22.00");
            assertThat(response.totalAmount()).isEqualByComparingTo("181.52");
        }

        @Test
        @DisplayName("6. repeating recalculation never compounds the fee")
        void repeatedRecalculationDoesNotCompound() {
            Invoice invoice = invoiceWithSubtotal("159.52", "0.00", today.minusDays(3), InvoiceStatus.PENDING);
            stubAccessible(invoice);

            invoiceService.recalculateLateFee(invoice.getId());
            BigDecimal afterFirst = captureSaved().getLateFee();
            invoiceService.recalculateLateFee(invoice.getId());
            BigDecimal afterSecond = captureSaved().getLateFee();
            invoiceService.recalculateLateFee(invoice.getId());
            BigDecimal afterThird = captureSaved().getLateFee();

            assertThat(afterFirst).isEqualByComparingTo("3.00");
            assertThat(afterSecond).isEqualByComparingTo(afterFirst);
            assertThat(afterThird).isEqualByComparingTo(afterFirst);
        }

        @Test
        @DisplayName("2. an invoice due today recalculates to 0.00")
        void dueTodayRecalculatesToZero() {
            Invoice invoice = invoiceWithSubtotal("159.52", "0.00", today, InvoiceStatus.PENDING);
            stubAccessible(invoice);

            invoiceService.recalculateLateFee(invoice.getId());

            assertThat(captureSaved().getLateFee()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("7. a partially paid overdue invoice reduces the remaining balance")
        void partialPaymentReducesRemaining() {
            Invoice invoice = invoiceWithSubtotal("159.52", "0.00", today.minusDays(22), InvoiceStatus.PARTIAL);
            stubAccessible(invoice);
            when(paymentRepository.findPaidTotalByInvoiceId(invoice.getId()))
                    .thenReturn(new BigDecimal("100.00"));

            InvoiceResponse response = invoiceService.recalculateLateFee(invoice.getId());

            assertThat(response.lateFee()).isEqualByComparingTo("22.00");
            assertThat(response.totalAmount()).isEqualByComparingTo("181.52");
            assertThat(response.paidAmount()).isEqualByComparingTo("100.00");
            assertThat(response.remainingAmount()).isEqualByComparingTo("81.52");
            assertThat(response.status()).isEqualTo(InvoiceStatus.PARTIAL);
        }

        @Test
        @DisplayName("8. only PAID payments count; the repository is queried with the PAID filter")
        void onlyPaidPaymentsCount() {
            Invoice invoice = invoiceWithSubtotal("159.52", "0.00", today.minusDays(22), InvoiceStatus.PARTIAL);
            stubAccessible(invoice);
            when(paymentRepository.findPaidTotalByInvoiceId(invoice.getId()))
                    .thenReturn(new BigDecimal("181.52"));

            InvoiceResponse response = invoiceService.recalculateLateFee(invoice.getId());

            assertThat(response.paidAmount()).isEqualByComparingTo("181.52");
            assertThat(response.remainingAmount()).isEqualByComparingTo("0.00");
            assertThat(response.status()).isEqualTo(InvoiceStatus.PAID);
        }

        @Test
        @DisplayName("9. a PAID invoice never receives a new late fee and stays PAID")
        void paidInvoiceNeverReceivesLateFee() {
            Invoice invoice = invoiceWithSubtotal("159.52", "0.00", today.minusDays(22), InvoiceStatus.PAID);
            invoice.setLateFee(BigDecimal.ZERO);
            stubAccessible(invoice);
            when(paymentRepository.findPaidTotalByInvoiceId(invoice.getId()))
                    .thenReturn(new BigDecimal("159.52"));

            InvoiceResponse response = invoiceService.recalculateLateFee(invoice.getId());

            Invoice saved = captureSaved();
            assertThat(saved.getLateFee()).isEqualByComparingTo("0.00");
            assertThat(saved.getStatus()).isEqualTo(InvoiceStatus.PAID);
            assertThat(response.remainingAmount()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("9. a PAID invoice cannot be edited to add a late fee")
        void paidInvoiceRejectsManualLateFee() {
            Invoice invoice = invoiceWithSubtotal("159.52", "0.00", today.minusDays(22), InvoiceStatus.PAID);
            stubAccessible(invoice);

            assertThatThrownBy(() -> invoiceService.updateLateFee(invoice.getId(), new BigDecimal("22.00")))
                    .isInstanceOf(InvalidInvoiceOperationException.class)
                    .hasMessageContaining("paid invoice");
            verify(invoiceRepository, never()).save(any());
        }

        @Test
        @DisplayName("10. the owner can reduce the fee and totals follow")
        void ownerCanReduceFee() {
            Invoice invoice = invoiceWithSubtotal("159.52", "0.00", today.minusDays(22), InvoiceStatus.PENDING);
            stubAccessible(invoice);

            InvoiceResponse response = invoiceService.updateLateFee(invoice.getId(), new BigDecimal("10.00"));

            Invoice saved = captureSaved();
            assertThat(saved.getLateFee()).isEqualByComparingTo("10.00");
            assertThat(saved.getTotalAmount()).isEqualByComparingTo("169.52");
            // The original subtotal and discount are untouched.
            assertThat(saved.getSubtotal()).isEqualByComparingTo("159.52");
            assertThat(saved.getDiscount()).isEqualByComparingTo("0.00");
            assertThat(response.lateFee()).isEqualByComparingTo("10.00");
            assertThat(response.remainingAmount()).isEqualByComparingTo("169.52");
        }

        @Test
        @DisplayName("11. the owner can waive the fee to 0.00")
        void ownerCanWaiveFee() {
            Invoice invoice = invoiceWithSubtotal("159.52", "0.00", today.minusDays(22), InvoiceStatus.PENDING);
            invoice.setLateFee(new BigDecimal("22.00"));
            stubAccessible(invoice);

            InvoiceResponse response = invoiceService.updateLateFee(invoice.getId(), BigDecimal.ZERO);

            assertThat(captureSaved().getLateFee()).isEqualByComparingTo("0.00");
            assertThat(response.totalAmount()).isEqualByComparingTo("159.52");
            assertThat(response.remainingAmount()).isEqualByComparingTo("159.52");
        }

        @Test
        @DisplayName("12. a negative late fee is rejected")
        void negativeLateFeeRejected() {
            Invoice invoice = invoiceWithSubtotal("159.52", "0.00", today.minusDays(22), InvoiceStatus.PENDING);
            stubAccessible(invoice);

            assertThatThrownBy(() -> invoiceService.updateLateFee(invoice.getId(), new BigDecimal("-1.00")))
                    .isInstanceOf(InvalidInvoiceOperationException.class)
                    .hasMessageContaining("must not be negative");
            verify(invoiceRepository, never()).save(any());
        }

        @Test
        @DisplayName("12. the request DTO rejects a negative amount at validation time")
        void requestDtoRejectsNegative() {
            UpdateInvoiceLateFeeRequest request = new UpdateInvoiceLateFeeRequest(new BigDecimal("-1.00"));
            assertThat(request.lateFee()).isEqualByComparingTo("-1.00");
        }

        @Test
        @DisplayName("13. an invoice outside the caller's owner scope is denied")
        void foreignInvoiceIsDenied() {
            Invoice invoice = invoiceWithSubtotal("159.52", "0.00", today.minusDays(22), InvoiceStatus.PENDING);
            invoice.getOwner().setId(99L);
            when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
            when(securityUtils.hasRole(any())).thenReturn(false);

            assertThatThrownBy(() -> invoiceService.updateLateFee(invoice.getId(), new BigDecimal("5.00")))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
            verify(invoiceRepository, never()).save(any());
        }

        @Test
        @DisplayName("13. the INVOICE_UPDATE permission is required")
        void permissionIsRequired() {
            Invoice invoice = invoiceWithSubtotal("159.52", "0.00", today.minusDays(22), InvoiceStatus.PENDING);
            stubAccessible(invoice);
            org.mockito.Mockito.doThrow(new org.springframework.security.access.AccessDeniedException("denied"))
                    .when(securityUtils).requirePermission(any());

            assertThatThrownBy(() -> invoiceService.updateLateFee(invoice.getId(), new BigDecimal("5.00")))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        }

        @Test
        @DisplayName("12. the DTO carries the documented validation annotations")
        void requestDtoShape() {
            var request = new UpdateInvoiceLateFeeRequest(new BigDecimal("10.00"));
            assertThat(request.lateFee()).isEqualByComparingTo("10.00");
            assertThat(new UpdateInvoiceLateFeeRequest(null).lateFee()).isNull();
        }

        @Test
        @DisplayName("mapper passes the stored fee straight through to the response")
        void mapperIsPassthrough() {
            Invoice invoice = invoiceWithSubtotal("159.52", "0.00", today.minusDays(22), InvoiceStatus.PENDING);
            invoice.setLateFee(new BigDecimal("22.00"));
            when(invoiceMapper.toResponse(eq(invoice), isNull(), any()))
                    .thenReturn(new InvoiceResponse(invoice.getId(), 1L, "INV-2026-0079", 30L, "CT-30",
                            null, null, 20L, "A-1", 10L, "Mansion", 2026, 9, today.minusDays(22),
                            new BigDecimal("159.52"), new BigDecimal("0.00"), new BigDecimal("22.00"),
                            new BigDecimal("181.52"), BigDecimal.ZERO, new BigDecimal("181.52"),
                            InvoiceStatus.PENDING, null, null, null, null, List.of()));

            InvoiceResponse response = invoiceMapper.toResponse(invoice, null, BigDecimal.ZERO);

            assertThat(response.lateFee()).isEqualByComparingTo("22.00");
            assertThat(response.totalAmount()).isEqualByComparingTo("181.52");
            assertThat(response.remainingAmount()).isEqualByComparingTo("181.52");
        }
    }

    // ---- helpers -------------------------------------------------------

    private Invoice invoiceWithSubtotal(String subtotal, String discount, LocalDate dueDate,
            InvoiceStatus status) {
        Invoice invoice = Invoice.builder()
                .id(106L)
                .owner(owner)
                .invoiceNo("INV-2026-0079")
                .contractId(30L)
                .billingYear(2026)
                .billingMonth(9)
                .dueDate(dueDate)
                .discount(new BigDecimal(discount))
                .subtotal(new BigDecimal(subtotal))
                .lateFee(BigDecimal.ZERO)
                .totalAmount(new BigDecimal(subtotal).subtract(new BigDecimal(discount)))
                .status(status)
                .build();
        InvoiceItem item = InvoiceItem.builder()
                .itemType(InvoiceItemType.RENT)
                .description("Monthly rent")
                .quantity(new BigDecimal("1"))
                .unitPrice(new BigDecimal(subtotal))
                .build();
        item.calculateAmount();
        invoice.addItem(item);
        return invoice;
    }

    private void stubAccessible(Invoice invoice) {
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(call -> call.getArgument(0));
        when(invoiceMapper.toResponse(any(Invoice.class), any(), any()))
                .thenAnswer(call -> {
                    Invoice inv = call.getArgument(0);
                    BigDecimal paid = call.getArgument(2);
                    return new InvoiceResponse(inv.getId(), 1L, inv.getInvoiceNo(), inv.getContractId(),
                            "CT-30", null, null, 20L, "A-1", 10L, "Mansion", inv.getBillingYear(),
                            inv.getBillingMonth(), inv.getDueDate(), inv.getSubtotal(), inv.getDiscount(),
                            inv.getLateFee(), inv.getTotalAmount(), paid,
                            InvoiceBalanceCalculator.remainingBalance(inv.getTotalAmount(), paid),
                            inv.getStatus(), null, null, null, null, List.of());
                });
    }

    private Invoice captureSaved() {
        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }
}
