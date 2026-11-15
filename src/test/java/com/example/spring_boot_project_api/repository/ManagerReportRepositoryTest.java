package com.example.spring_boot_project_api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.TestPropertySource;

import com.example.spring_boot_project_api.enums.InvoiceItemType;
import com.example.spring_boot_project_api.enums.InvoiceStatus;
import com.example.spring_boot_project_api.enums.PaymentStatus;

/**
 * Executes every manager-report aggregation query with an empty house scope.
 * This forces Hibernate to parse and compile the JPQL at runtime (Spring Data
 * validates @Query methods lazily), verifying the generated SQL works against
 * the configured database without requiring seeded fixtures.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.flyway.clean-disabled=true"
})
class ManagerReportRepositoryTest {

    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private RentalContractRepository rentalContractRepository;
    @Autowired private ContractRenewalRepository contractRenewalRepository;
    @Autowired private ContractDepositRepository contractDepositRepository;
    @Autowired private MaintenanceRequestRepository maintenanceRequestRepository;
    @Autowired private MaintenanceCostRepository maintenanceCostRepository;
    @Autowired private UtilityReadingRepository utilityReadingRepository;

    private final List<Long> noHouses = List.of();
    private final LocalDateTime start = LocalDateTime.of(2026, 9, 1, 0, 0);
    private final LocalDateTime end = LocalDateTime.of(2026, 10, 1, 0, 0);

    @Test
    void invoiceReportQueriesCompileWithEmptyScope() {
        assertThat(invoiceRepository
                .sumRentAmountByYearMonthHouseIds(2026, 9, noHouses))
                .isEqualByComparingTo("0");
        assertThat(invoiceRepository
                .sumOutstandingAmountByYearMonthHouseIds(2026, 9, noHouses,
                        List.of(InvoiceStatus.PENDING, InvoiceStatus.PARTIAL, InvoiceStatus.OVERDUE)))
                .isEqualByComparingTo("0");
        assertThat(invoiceRepository
                .sumItemAmountsByYearMonthHouseIds(2026, 9, noHouses,
                        List.of(InvoiceItemType.WATER, InvoiceItemType.ELECTRICITY)))
                .isEqualByComparingTo("0");
        assertThat(invoiceRepository
                .countUtilityBillsByYearMonthHouseIdsAndStatusIn(2026, 9, noHouses,
                        List.of(InvoiceStatus.PAID),
                        List.of(InvoiceItemType.WATER, InvoiceItemType.ELECTRICITY)))
                .isZero();
        assertThat(invoiceRepository.countGroupByStatusByYearMonthHouseIds(2026, 9, noHouses))
                .isEmpty();
    }

    @Test
    void paymentReportQueriesCompileWithEmptyScope() {
        assertThat(paymentRepository
                .sumPaidAmountByHouseIdsAndPaidAtRange(start, end, noHouses))
                .isEqualByComparingTo("0");
        assertThat(paymentRepository
                .countByHouseIdsAndStatusAndPaidAtRange(start, end, noHouses, PaymentStatus.PAID))
                .isZero();
        assertThat(paymentRepository
                .countByHouseIdsAndStatusAndCreatedAtRange(start, end, noHouses, PaymentStatus.PENDING))
                .isZero();
        assertThat(paymentRepository
                .sumPaidGroupByYearMonthByHouseIds(
                        LocalDateTime.of(2025, 10, 1, 0, 0), end, noHouses))
                .isEmpty();
    }

    @Test
    void contractDepositRenewalQueriesCompileWithEmptyScope() {
        LocalDate day = LocalDate.of(2026, 9, 1);
        assertThat(rentalContractRepository.countCreatedByHouseIdsAndRange(start, end, noHouses))
                .isZero();
        assertThat(rentalContractRepository
                .countActiveOverlappingMonthByHouseIds(day, day.plusMonths(1), noHouses)).isZero();
        assertThat(rentalContractRepository
                .countExpiredWithinMonthByHouseIds(day, day.plusMonths(1), noHouses)).isZero();
        assertThat(rentalContractRepository
                .countTerminatedWithinMonthByHouseIds(day, day.plusMonths(1), noHouses)).isZero();
        assertThat(contractRenewalRepository.countByHouseIdsAndRenewedAtRange(start, end, noHouses))
                .isZero();
        assertThat(contractDepositRepository.sumPaidAmountByHouseIdsAndPaidAtRange(start, end, noHouses))
                .isEqualByComparingTo("0");
    }

    @Test
    void maintenanceUtilityQueriesCompileWithEmptyScope() {
        assertThat(maintenanceRequestRepository
                .countGroupByStatusAndHouseIdsAndReportedAtRange(start, end, noHouses)).isEmpty();
        assertThat(maintenanceCostRepository.sumTotalAmountByHouseIdsAndCreatedAtRange(start, end, noHouses))
                .isEqualByComparingTo("0");
        assertThat(utilityReadingRepository
                .sumUsageGroupByTypeCodeByHouseIdsAndDateRange(
                        start.toLocalDate(), end.toLocalDate(), noHouses))
                .isEmpty();
    }
}