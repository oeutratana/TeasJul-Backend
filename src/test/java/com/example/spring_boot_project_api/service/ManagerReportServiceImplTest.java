package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import com.example.spring_boot_project_api.dto.response.ManagerMonthlyReportResponse;
import com.example.spring_boot_project_api.enums.InvoiceStatus;
import com.example.spring_boot_project_api.enums.PaymentStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.exception.InvalidReportQueryException;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.Invoice;
import com.example.spring_boot_project_api.repository.ContractDepositRepository;
import com.example.spring_boot_project_api.repository.ContractRenewalRepository;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.InvoiceRepository;
import com.example.spring_boot_project_api.repository.MaintenanceCostRepository;
import com.example.spring_boot_project_api.repository.MaintenanceRequestRepository;
import com.example.spring_boot_project_api.repository.PaymentRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.UtilityReadingRepository;
import com.example.spring_boot_project_api.service.impl.ManagerReportServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ManagerReportServiceImplTest {

    @Mock private RoomRepository roomRepository;
    @Mock private HouseRepository houseRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private RentalContractRepository contractRepository;
    @Mock private ContractRenewalRepository contractRenewalRepository;
    @Mock private ContractDepositRepository contractDepositRepository;
    @Mock private MaintenanceRequestRepository maintenanceRequestRepository;
    @Mock private MaintenanceCostRepository maintenanceCostRepository;
    @Mock private UtilityReadingRepository utilityReadingRepository;
    @Mock private HouseAccessService houseAccessService;

    @InjectMocks private ManagerReportServiceImpl service;

    @Test
    void getMonthlyReport_noAccessibleHouses_returnsSafeEmptyState() {
        when(houseAccessService.getAccessibleHouseIds()).thenReturn(List.of());

        ManagerMonthlyReportResponse report = service.getMonthlyReport(2026, 9, null);

        assertThat(report.rooms().totalRooms()).isZero();
        assertThat(report.financial().netRevenue()).isEqualByComparingTo("0");
        assertThat(report.contracts().activeContracts()).isZero();
        assertThat(report.payments().paidPayments()).isZero();
        assertThat(report.utilities().totalUtilityCharges()).isEqualByComparingTo("0");
        assertThat(report.maintenance().totalRequests()).isZero();
        assertThat(report.revenueTrend()).isEmpty();
        assertThat(report.paymentStatus()).hasSize(InvoiceStatus.values().length);
        assertThat(report.maintenanceStatus()).hasSize(4);
    }

    @Test
    void getMonthlyReport_rejectsHouseOutsideScope() {
        when(houseAccessService.getAccessibleHouseIds()).thenReturn(List.of(8L, 9L));
        org.mockito.Mockito.doThrow(new AccessDeniedException("No access"))
                .when(houseAccessService).requireHouseAccess(99L);

        assertThatThrownBy(() -> service.getMonthlyReport(2026, 9, 99L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getMonthlyReport_invalidPeriod_throwsInvalidReportQueryException() {
        assertThatThrownBy(() -> service.getMonthlyReport(2026, 13, null))
                .isInstanceOf(InvalidReportQueryException.class);
        assertThatThrownBy(() -> service.getMonthlyReport(1999, 5, null))
                .isInstanceOf(InvalidReportQueryException.class);
    }

    @Test
    void getMonthlyReport_allHouses_computesSummariesAndCharts() {
        stubScope();

        when(roomRepository.countByHouseIdInAndStatus(anyList(), eq(RoomStatus.OCCUPIED)))
                .thenReturn(3L);
        when(roomRepository.countByHouseIdInAndStatus(anyList(), eq(RoomStatus.AVAILABLE)))
                .thenReturn(2L);
        when(roomRepository.countByHouseIdInAndStatus(anyList(), eq(RoomStatus.MAINTENANCE)))
                .thenReturn(1L);
        when(roomRepository.countByHouseIdInAndStatus(anyList(), eq(RoomStatus.RESERVED)))
                .thenReturn(0L);

        when(invoiceRepository.sumRentAmountByYearMonthHouseIds(anyInt(), anyInt(), anyList()))
                .thenReturn(new BigDecimal("3000.00"));
        when(paymentRepository.sumPaidAmountByHouseIdsAndPaidAtRange(any(), any(), anyList()))
                .thenReturn(new BigDecimal("1800.00"));
        when(invoiceRepository.sumOutstandingAmountByYearMonthHouseIds(anyInt(), anyInt(), anyList(), anyList()))
                .thenReturn(new BigDecimal("900.00"));
        when(contractDepositRepository.sumPaidAmountByHouseIdsAndPaidAtRange(any(), any(), anyList()))
                .thenReturn(new BigDecimal("500.00"));
        when(maintenanceCostRepository.sumTotalAmountByHouseIdsAndCreatedAtRange(any(), any(), anyList()))
                .thenReturn(new BigDecimal("120.00"));

        when(contractRepository.countCreatedByHouseIdsAndRange(any(), any(), anyList())).thenReturn(4L);
        when(contractRepository.countActiveOverlappingMonthByHouseIds(any(), any(), anyList())).thenReturn(25L);
        when(contractRepository.countExpiredWithinMonthByHouseIds(any(), any(), anyList())).thenReturn(2L);
        when(contractRepository.countTerminatedWithinMonthByHouseIds(any(), any(), anyList())).thenReturn(1L);
        when(contractRenewalRepository.countByHouseIdsAndRenewedAtRange(any(), any(), anyList())).thenReturn(3L);

        when(paymentRepository.countByHouseIdsAndStatusAndPaidAtRange(any(), any(), anyList(), eq(PaymentStatus.PAID)))
                .thenReturn(40L);
        when(paymentRepository.countByHouseIdsAndStatusAndCreatedAtRange(any(), any(), anyList(), eq(PaymentStatus.PENDING)))
                .thenReturn(10L);
        when(invoiceRepository.countByYearMonthHouseIdsAndStatus(anyInt(), anyInt(), anyList(), eq(InvoiceStatus.OVERDUE)))
                .thenReturn(5L);

        when(utilityReadingRepository.sumUsageGroupByTypeCodeByHouseIdsAndDateRange(any(), any(), anyList()))
                .thenReturn(List.of(
                        new Object[]{"WATER", new BigDecimal("150.00")},
                        new Object[]{"ELECTRICITY", new BigDecimal("260.00")}));
        when(invoiceRepository.sumItemAmountsByYearMonthHouseIds(anyInt(), anyInt(), anyList(), anyList()))
                .thenReturn(new BigDecimal("210.00"));
        when(invoiceRepository.countUtilityBillsByYearMonthHouseIdsAndStatusIn(
                anyInt(), anyInt(), anyList(), eq(List.of(InvoiceStatus.PAID)), anyList()))
                .thenReturn(18L);
        when(invoiceRepository.countUtilityBillsByYearMonthHouseIdsAndStatusIn(
                anyInt(), anyInt(), anyList(), eq(List.of(InvoiceStatus.PENDING, InvoiceStatus.PARTIAL, InvoiceStatus.OVERDUE)), anyList()))
                .thenReturn(7L);

        when(maintenanceRequestRepository.countGroupByStatusAndHouseIdsAndReportedAtRange(any(), any(), anyList()))
                .thenReturn(List.of(
                        new Object[]{"PENDING", 2L},
                        new Object[]{"IN_PROGRESS", 3L},
                        new Object[]{"RESOLVED", 5L}));


        when(paymentRepository.sumPaidAmountByInvoiceIds(anyList(), any()))
                .thenAnswer(inv -> {
                    List<Object[]> rows = new ArrayList<>();
                    for (int i = 0; i < 5; i++) {
                        Long invoiceId = (i + 1L);
                        rows.add(new Object[]{invoiceId, new BigDecimal("50.00")});
                    }
                    return rows;
                });

        when(invoiceRepository.findByYearMonthHouseIds(anyInt(), anyInt(), anyList()))
                .thenAnswer(invocation -> {
                    List<Invoice> invoices = new ArrayList<>();
                    for (int i = 0; i < 5; i++) {
                        Invoice inv = new Invoice();
                        inv.setId(i + 1L);
                        inv.setDueDate(LocalDate.of(2026, 9, 1 + i));
                        inv.setTotalAmount(new BigDecimal("100.00"));
                        inv.setStatus(InvoiceStatus.OVERDUE);
                        invoices.add(inv);
                    }
                    return invoices;
                });

        when(paymentRepository.sumPaidGroupByYearMonthByHouseIds(any(), any(), anyList()))
                .thenReturn(List.of(
                        new Object[]{2026, 8, new BigDecimal("1500.00")},
                        new Object[]{2026, 9, new BigDecimal("1800.00")}));

        when(invoiceRepository.countGroupByStatusByYearMonthHouseIds(anyInt(), anyInt(), anyList()))
                .thenReturn(List.of(
                        new Object[]{"PAID", 30L},
                        new Object[]{"OVERDUE", 5L},
                        new Object[]{"PENDING", 8L}));

        ManagerMonthlyReportResponse report = service.getMonthlyReport(2026, 9, null);

        assertThat(report.meta().year()).isEqualTo(2026);
        assertThat(report.meta().month()).isEqualTo(9);
        assertThat(report.meta().periodStart()).isEqualTo(LocalDateTime.of(2026, 9, 1, 0, 0));
        assertThat(report.meta().periodEnd()).isEqualTo(LocalDateTime.of(2026, 10, 1, 0, 0));

        assertThat(report.rooms().totalRooms()).isEqualTo(6);
        assertThat(report.rooms().occupiedRooms()).isEqualTo(3);
        assertThat(report.rooms().occupancyRate()).isEqualTo(50.0);

        assertThat(report.financial().totalRent()).isEqualByComparingTo("3000.00");
        assertThat(report.financial().totalPaid()).isEqualByComparingTo("1800.00");
        assertThat(report.financial().totalOutstanding()).isEqualByComparingTo("900.00");
        assertThat(report.financial().totalDeposits()).isEqualByComparingTo("500.00");
        assertThat(report.financial().totalExpenses()).isEqualByComparingTo("120.00");
        assertThat(report.financial().netRevenue()).isEqualByComparingTo("2180.00");

        assertThat(report.contracts().newContracts()).isEqualTo(4);
        assertThat(report.contracts().activeContracts()).isEqualTo(25);
        assertThat(report.contracts().expiredContracts()).isEqualTo(2);
        assertThat(report.contracts().terminatedContracts()).isEqualTo(1);
        assertThat(report.contracts().renewedContracts()).isEqualTo(3);

        assertThat(report.payments().paidPayments()).isEqualTo(40);
        assertThat(report.payments().pendingPayments()).isEqualTo(10);
        assertThat(report.payments().overduePayments()).isEqualTo(5);
        assertThat(report.payments().totalPaymentAmount()).isEqualByComparingTo("1800.00");

        assertThat(report.utilities().totalWaterUsage()).isEqualByComparingTo("150.00");
        assertThat(report.utilities().totalElectricityUsage()).isEqualByComparingTo("260.00");
        assertThat(report.utilities().totalUtilityCharges()).isEqualByComparingTo("210.00");
        assertThat(report.utilities().paidUtilityBills()).isEqualTo(18);
        assertThat(report.utilities().outstandingUtilityBills()).isEqualTo(7);

        assertThat(report.maintenance().totalRequests()).isEqualTo(10);
        assertThat(report.maintenance().openRequests()).isEqualTo(2);
        assertThat(report.maintenance().inProgressRequests()).isEqualTo(3);
        assertThat(report.maintenance().completedRequests()).isEqualTo(5);
        assertThat(report.maintenance().cancelledRequests()).isZero();
        assertThat(report.maintenance().totalCost()).isEqualByComparingTo("120.00");

        assertThat(report.revenueTrend()).hasSize(12);
        assertThat(report.revenueTrend().get(0).month()).isEqualTo("2025-10");
        assertThat(report.revenueTrend().get(0).amount()).isEqualByComparingTo("0");
        assertThat(report.revenueTrend())
                .filteredOn(p -> p.month().equals("2026-08"))
                .singleElement()
                .extracting(p -> p.amount())
                .isEqualTo(new BigDecimal("1500.00"));
        assertThat(report.revenueTrend().get(11).month()).isEqualTo("2026-09");
        assertThat(report.revenueTrend().get(11).amount()).isEqualByComparingTo("1800.00");

        assertThat(report.paymentStatus())
                .extracting(s -> s.status())
                .containsExactly("CANCELLED", "DRAFT", "OVERDUE", "PAID", "PARTIAL", "PENDING");
        assertThat(report.paymentStatus())
                .filteredOn(s -> s.status().equals("PAID"))
                .singleElement()
                .extracting(s -> s.count())
                .isEqualTo(30L);
        assertThat(report.paymentStatus())
                .filteredOn(s -> s.status().equals("DRAFT"))
                .singleElement()
                .extracting(s -> s.count())
                .isEqualTo(0L);

        assertThat(report.roomOccupancy())
                .extracting(s -> s.status())
                .containsExactly("AVAILABLE", "BOOKED", "MAINTENANCE", "OCCUPIED", "RESERVED");
        assertThat(report.roomOccupancy())
                .filteredOn(s -> s.status().equals("OCCUPIED"))
                .singleElement()
                .extracting(s -> s.count())
                .isEqualTo(3L);

        assertThat(report.maintenanceStatus())
                .extracting(s -> s.status())
                .containsExactly("CANCELLED", "IN_PROGRESS", "PENDING", "RESOLVED");
    }

    @Test
    void getMonthlyReport_singleHouse_scopesAndResolvesHouseName() {
        when(houseAccessService.getAccessibleHouseIds()).thenReturn(List.of(8L, 9L));
        House house = new House();
        house.setId(8L);
        house.setName("Teas Jul Villa");
        when(houseRepository.findById(8L)).thenReturn(java.util.Optional.of(house));

        when(roomRepository.countByHouseIdInAndStatus(eq(List.of(8L)), eq(RoomStatus.OCCUPIED)))
                .thenReturn(2L);
        when(roomRepository.countByHouseIdInAndStatus(eq(List.of(8L)), eq(RoomStatus.AVAILABLE)))
                .thenReturn(1L);
        when(roomRepository.countByHouseIdInAndStatus(eq(List.of(8L)), eq(RoomStatus.MAINTENANCE)))
                .thenReturn(1L);
        when(roomRepository.countByHouseIdInAndStatus(eq(List.of(8L)), eq(RoomStatus.RESERVED)))
                .thenReturn(0L);
        when(paymentRepository.sumPaidGroupByYearMonthByHouseIds(any(), any(), eq(List.of(8L))))
                .thenReturn(List.of());
        when(invoiceRepository.sumRentAmountByYearMonthHouseIds(anyInt(), anyInt(), anyList()))
                .thenReturn(BigDecimal.ZERO);
        when(paymentRepository.sumPaidAmountByHouseIdsAndPaidAtRange(any(), any(), anyList()))
                .thenReturn(BigDecimal.ZERO);
        when(invoiceRepository.sumOutstandingAmountByYearMonthHouseIds(anyInt(), anyInt(), anyList(), anyList()))
                .thenReturn(BigDecimal.ZERO);
        when(contractDepositRepository.sumPaidAmountByHouseIdsAndPaidAtRange(any(), any(), anyList()))
                .thenReturn(BigDecimal.ZERO);
        when(maintenanceCostRepository.sumTotalAmountByHouseIdsAndCreatedAtRange(any(), any(), anyList()))
                .thenReturn(BigDecimal.ZERO);
        when(invoiceRepository.sumItemAmountsByYearMonthHouseIds(anyInt(), anyInt(), anyList(), anyList()))
                .thenReturn(BigDecimal.ZERO);

        ManagerMonthlyReportResponse report = service.getMonthlyReport(2026, 9, 8L);

        assertThat(report.meta().houseId()).isEqualTo(8L);
        assertThat(report.meta().houseName()).isEqualTo("Teas Jul Villa");
        assertThat(report.rooms().occupiedRooms()).isEqualTo(2);
        assertThat(report.rooms().totalRooms()).isEqualTo(4);
        assertThat(report.revenueTrend()).hasSize(12);
    }

    private void stubScope() {
        when(houseAccessService.getAccessibleHouseIds()).thenReturn(List.of(8L, 9L));
    }
}