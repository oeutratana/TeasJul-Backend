package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import com.example.spring_boot_project_api.dto.response.ManagerDashboardResponse;
import com.example.spring_boot_project_api.enums.InvoiceStatus;
import com.example.spring_boot_project_api.enums.PaymentStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.model.Invoice;
import com.example.spring_boot_project_api.model.MaintenanceRequest;
import com.example.spring_boot_project_api.model.MaintenanceRequestStatus;
import com.example.spring_boot_project_api.model.Payment;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.repository.InvoiceRepository;
import com.example.spring_boot_project_api.repository.MaintenanceRequestRepository;
import com.example.spring_boot_project_api.repository.PaymentRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.service.impl.ManagerDashboardServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ManagerDashboardServiceImplTest {

    @Mock private RoomRepository roomRepository;
    @Mock private RentalContractRepository contractRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private MaintenanceRequestRepository maintenanceRequestRepository;
    @Mock private HouseAccessService houseAccessService;

    @InjectMocks private ManagerDashboardServiceImpl service;

    @Test
    void getDashboard_noAccessibleHouses_returnsSafeEmptyState() {
        when(houseAccessService.getAccessibleHouseIds()).thenReturn(List.of());

        ManagerDashboardResponse response = service.getDashboard(null, null, null, null, null);

        assertThat(response.kpis().totalRooms()).isZero();
        assertThat(response.kpis().revenue()).isEqualByComparingTo("0");
        assertThat(response.revenueTrend()).isEmpty();
        assertThat(response.roomOccupancy()).isEmpty();
        assertThat(response.paymentStatus()).isEmpty();
        assertThat(response.maintenanceStatus()).isEmpty();
    }

    @Test
    void getDashboard_rejectsHouseOutsideScope() {
        when(houseAccessService.getAccessibleHouseIds()).thenReturn(List.of(8L, 9L));
        org.mockito.Mockito.doThrow(new AccessDeniedException("No access"))
                .when(houseAccessService).requireHouseAccess(99L);

        assertThatThrownBy(() -> service.getDashboard(99L, null, null, null, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getDashboard_allHouses_computesKpisAndCharts() {
        stubScope();
        stubMultiHouseRoomCounts();

        when(contractRepository.findByRoomHouseIdIn(List.of(8L, 9L)))
                .thenReturn(List.of(contract(100L, 10L), contract(200L, 11L)));

        LocalDate today = LocalDate.now();
        List<Invoice> scopeInvoices = List.of(
                invoice(1L, InvoiceStatus.PAID, LocalDateTime.of(2026, 5, 10, 8, 0),
                        today.minusDays(10), "100.00"),
                invoice(2L, InvoiceStatus.OVERDUE, LocalDateTime.of(2026, 6, 5, 12, 0),
                        today.minusDays(5), "200.00"),
                invoice(3L, InvoiceStatus.PENDING, LocalDateTime.of(2026, 4, 20, 9, 0),
                        today.plusDays(5), "50.00"));
        when(invoiceRepository.findByContractIdIn(List.of(100L, 200L))).thenReturn(scopeInvoices);

        when(paymentRepository.findByInvoiceIdIn(List.of(1L))).thenReturn(List.of(
                payment(1L, 1L, "100.00", PaymentStatus.PAID, LocalDateTime.of(2026, 5, 10, 8, 30)),
                payment(2L, 1L, "50.00", PaymentStatus.PAID, LocalDateTime.of(2026, 6, 10, 8, 30)),
                payment(3L, 1L, "80.00", PaymentStatus.PAID, LocalDateTime.of(2026, 7, 5, 8, 30))));

        // One grouped query for the whole scope: only invoice 1 is settled, so invoice 2 is the
        // single overdue one (past due, still owing) and invoice 3 is not due yet.
        when(paymentRepository.sumPaidAmountByInvoiceIds(List.of(1L, 2L, 3L), PaymentStatus.PAID))
                .thenReturn(List.<Object[]>of(new Object[] { 1L, new BigDecimal("100.00") }));

        when(maintenanceRequestRepository.findAllByHouseIdInOrderByCreatedAtDesc(List.of(8L, 9L)))

                .thenReturn(List.of(
                        maintenanceRequest(1L, MaintenanceRequestStatus.PENDING,
                                LocalDateTime.of(2026, 5, 1, 8, 0)),
                        maintenanceRequest(2L, MaintenanceRequestStatus.RESOLVED,
                                LocalDateTime.of(2026, 3, 1, 8, 0))));

        ManagerDashboardResponse dashboard = service.getDashboard(null,
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30), null, null);

        assertThat(dashboard.kpis().totalRooms()).isEqualTo(6);
        assertThat(dashboard.kpis().availableRooms()).isEqualTo(2);
        assertThat(dashboard.kpis().occupiedRooms()).isEqualTo(3);
        assertThat(dashboard.kpis().maintenanceRooms()).isEqualTo(1);
        assertThat(dashboard.kpis().revenue()).isEqualByComparingTo("150.00");
        assertThat(dashboard.kpis().occupancyRate()).isEqualTo(50.0);
        assertThat(dashboard.kpis().pendingInvoices()).isEqualTo(1);
        assertThat(dashboard.kpis().overdueInvoices()).isEqualTo(1);

        assertThat(dashboard.revenueTrend()).hasSize(3);
        assertThat(dashboard.revenueTrend().get(0).month()).isEqualTo("2026-04");
        assertThat(dashboard.revenueTrend().get(0).amount()).isEqualByComparingTo("0");
        assertThat(dashboard.revenueTrend().get(1).month()).isEqualTo("2026-05");
        assertThat(dashboard.revenueTrend().get(1).amount()).isEqualByComparingTo("100.00");
        assertThat(dashboard.revenueTrend().get(2).month()).isEqualTo("2026-06");
        assertThat(dashboard.revenueTrend().get(2).amount()).isEqualByComparingTo("50.00");

        assertThat(dashboard.roomOccupancy()).hasSize(2);
        assertThat(dashboard.roomOccupancy().get(0).label()).isEqualTo("House #8");
        assertThat(dashboard.roomOccupancy().get(0).occupied()).isEqualTo(2);
        assertThat(dashboard.roomOccupancy().get(0).available()).isEqualTo(1);
        assertThat(dashboard.roomOccupancy().get(0).maintenance()).isEqualTo(1);
        assertThat(dashboard.roomOccupancy().get(1).label()).isEqualTo("House #9");
        assertThat(dashboard.roomOccupancy().get(1).occupied()).isEqualTo(1);
        assertThat(dashboard.roomOccupancy().get(1).maintenance()).isZero();

        assertThat(dashboard.paymentStatus())
                .extracting(s -> s.status())
                .containsExactly("CANCELLED", "DRAFT", "OVERDUE", "PAID", "PARTIAL", "PENDING");
        assertThat(dashboard.paymentStatus())
                .filteredOn(s -> s.status().equals("PAID"))
                .singleElement()
                .extracting(s -> s.count())
                .isEqualTo(1L);
        assertThat(dashboard.paymentStatus())
                .filteredOn(s -> s.status().equals("OVERDUE"))
                .singleElement()
                .extracting(s -> s.count())
                .isEqualTo(1L);

        assertThat(dashboard.maintenanceStatus())
                .anyMatch(s -> s.status().equals("PENDING") && s.count() == 1);
        assertThat(dashboard.maintenanceStatus())
                .anyMatch(s -> s.status().equals("RESOLVED") && s.count() == 0);
        assertThat(dashboard.maintenanceStatus()).hasSize(4);

        // Overdue aggregation must stay a single grouped payment query, never one per invoice.
        verify(paymentRepository, times(1))
                .sumPaidAmountByInvoiceIds(List.of(1L, 2L, 3L), PaymentStatus.PAID);
    }

    @Test
    void getDashboard_overdueCountDerivedFromDueDateAndOutstandingBalance() {
        stubScope();
        stubMultiHouseRoomCounts();
        when(contractRepository.findByRoomHouseIdIn(List.of(8L, 9L)))
                .thenReturn(List.of(contract(100L, 10L)));
        when(maintenanceRequestRepository.findAllByHouseIdInOrderByCreatedAtDesc(List.of(8L, 9L)))
                .thenReturn(List.of());

        LocalDate today = LocalDate.now();
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 1, 8, 0);
        List<Invoice> invoices = List.of(
                // Past due and still owing -> overdue
                invoice(1L, InvoiceStatus.PENDING, createdAt, today.minusDays(1), "100.00"),
                // Past due but fully settled -> not overdue
                invoice(2L, InvoiceStatus.PAID, createdAt, today.minusDays(1), "100.00"),
                // Due today is not yet late
                invoice(3L, InvoiceStatus.PENDING, createdAt, today, "100.00"),
                // Not due yet
                invoice(4L, InvoiceStatus.PENDING, createdAt, today.plusDays(7), "100.00"),
                // Partially paid but still owing -> overdue
                invoice(5L, InvoiceStatus.PARTIAL, createdAt, today.minusDays(3), "100.00"),
                // Cancelled invoices are closed
                invoice(6L, InvoiceStatus.CANCELLED, createdAt, today.minusDays(9), "100.00"));
        when(invoiceRepository.findByContractIdIn(List.of(100L))).thenReturn(invoices);

        when(paymentRepository.sumPaidAmountByInvoiceIds(List.of(1L, 2L, 3L, 4L, 5L, 6L),
                PaymentStatus.PAID))
                .thenReturn(List.<Object[]>of(
                        new Object[] { 2L, new BigDecimal("100.00") },
                        new Object[] { 5L, new BigDecimal("40.00") }));

        ManagerDashboardResponse dashboard = service.getDashboard(null, null, null, null, null);

        assertThat(dashboard.kpis().overdueInvoices()).isEqualTo(2L);
    }

    @Test
    void getDashboard_singleHouse_groupsAllRooms() {
        when(houseAccessService.getAccessibleHouseIds()).thenReturn(List.of(8L, 9L));

        when(roomRepository.countByHouseIdInAndStatus(eq(List.of(8L)), eq(RoomStatus.OCCUPIED)))
                .thenReturn(2L);
        when(roomRepository.countByHouseIdInAndStatus(eq(List.of(8L)), eq(RoomStatus.AVAILABLE)))
                .thenReturn(1L);
        when(roomRepository.countByHouseIdInAndStatus(eq(List.of(8L)), eq(RoomStatus.MAINTENANCE)))
                .thenReturn(1L);
        when(contractRepository.findByRoomHouseIdIn(List.of(8L)))
                .thenReturn(List.of(contract(100L, 10L)));
        when(invoiceRepository.findByContractIdIn(List.of(100L))).thenReturn(List.of());
        when(maintenanceRequestRepository.findAllByHouseIdInOrderByCreatedAtDesc(List.of(8L)))
                .thenReturn(List.of());

        ManagerDashboardResponse dashboard = service.getDashboard(8L, null, null, null, null);

        assertThat(dashboard.kpis().totalRooms()).isEqualTo(4);
        assertThat(dashboard.roomOccupancy()).hasSize(1);
        assertThat(dashboard.roomOccupancy().get(0).label()).isEqualTo("បន្ទប់ទាំងអស់");
        assertThat(dashboard.roomOccupancy().get(0).occupied()).isEqualTo(2);
        assertThat(dashboard.roomOccupancy().get(0).available()).isEqualTo(1);
        assertThat(dashboard.roomOccupancy().get(0).maintenance()).isEqualTo(1);
    }

    @Test
    void getDashboard_roomStatusFilterRestrictsRoomMetrics() {
        stubScope();
        stubMultiHouseRoomCounts();
        when(contractRepository.findByRoomHouseIdIn(List.of(8L, 9L)))
                .thenReturn(List.of(contract(100L, 10L), contract(200L, 11L)));
        when(invoiceRepository.findByContractIdIn(List.of(100L, 200L))).thenReturn(List.of());
        when(maintenanceRequestRepository.findAllByHouseIdInOrderByCreatedAtDesc(List.of(8L, 9L)))
                .thenReturn(List.of());

        ManagerDashboardResponse dashboard = service.getDashboard(null, null, null, RoomStatus.OCCUPIED, null);

        assertThat(dashboard.kpis().totalRooms()).isEqualTo(3);
        assertThat(dashboard.kpis().occupiedRooms()).isEqualTo(3);
        assertThat(dashboard.kpis().availableRooms()).isZero();
        assertThat(dashboard.kpis().maintenanceRooms()).isZero();
        assertThat(dashboard.kpis().occupancyRate()).isEqualTo(100.0);
        assertThat(dashboard.roomOccupancy().get(0).occupied()).isEqualTo(2);
        assertThat(dashboard.roomOccupancy().get(0).available()).isZero();
        assertThat(dashboard.roomOccupancy().get(1).occupied()).isEqualTo(1);
        assertThat(dashboard.roomOccupancy().get(1).available()).isZero();
    }

    @Test
    void getDashboard_paymentStatusFilterRestrictsDonutButNotRevenue() {
        stubScope();
        stubMultiHouseRoomCounts();
        when(contractRepository.findByRoomHouseIdIn(List.of(8L, 9L)))
                .thenReturn(List.of(contract(100L, 10L)));
        List<Invoice> scopeInvoices = List.of(
                invoice(1L, InvoiceStatus.PAID, LocalDateTime.of(2026, 5, 10, 8, 0)),
                invoice(2L, InvoiceStatus.OVERDUE, LocalDateTime.of(2026, 6, 5, 12, 0)));
        when(invoiceRepository.findByContractIdIn(List.of(100L))).thenReturn(scopeInvoices);
        when(paymentRepository.findByInvoiceIdIn(List.of(1L)))
                .thenReturn(List.of(payment(1L, 1L, "100.00", PaymentStatus.PAID,
                        LocalDateTime.of(2026, 5, 10, 8, 30))));
        when(maintenanceRequestRepository.findAllByHouseIdInOrderByCreatedAtDesc(List.of(8L, 9L)))
                .thenReturn(List.of());

        ManagerDashboardResponse dashboard = service.getDashboard(null, null, null, null, InvoiceStatus.PAID);

        assertThat(dashboard.kpis().pendingInvoices()).isZero();
        assertThat(dashboard.kpis().overdueInvoices()).isZero();
        assertThat(dashboard.kpis().revenue()).isEqualByComparingTo("100.00");
        assertThat(dashboard.paymentStatus())
                .extracting(s -> s.status())
                .contains("CANCELLED", "DRAFT", "OVERDUE", "PAID", "PARTIAL", "PENDING");
        assertThat(dashboard.paymentStatus())
                .filteredOn(s -> s.status().equals("PAID"))
                .singleElement()
                .extracting(s -> s.count())
                .isEqualTo(1L);
        assertThat(dashboard.paymentStatus())
                .filteredOn(s -> s.status().equals("PENDING"))
                .singleElement()
                .extracting(s -> s.count())
                .isEqualTo(0L);
    }

    private void stubScope() {
        when(houseAccessService.getAccessibleHouseIds()).thenReturn(List.of(8L, 9L));
    }

    private void stubMultiHouseRoomCounts() {
        when(roomRepository.countByHouseIdInAndStatus(anyList(), eq(RoomStatus.OCCUPIED)))
                .thenReturn(3L);
        when(roomRepository.countByHouseIdInAndStatus(anyList(), eq(RoomStatus.AVAILABLE)))
                .thenReturn(2L);
        when(roomRepository.countByHouseIdInAndStatus(anyList(), eq(RoomStatus.MAINTENANCE)))
                .thenReturn(1L);
        when(roomRepository.countByHouseIdInAndStatus(anyList(), eq(RoomStatus.RESERVED)))
                .thenReturn(0L);
        when(roomRepository.countByHouseIdInAndStatus(eq(List.of(8L)), eq(RoomStatus.OCCUPIED)))
                .thenReturn(2L);
        when(roomRepository.countByHouseIdInAndStatus(eq(List.of(8L)), eq(RoomStatus.AVAILABLE)))
                .thenReturn(1L);
        when(roomRepository.countByHouseIdInAndStatus(eq(List.of(8L)), eq(RoomStatus.MAINTENANCE)))
                .thenReturn(1L);
        when(roomRepository.countByHouseIdInAndStatus(eq(List.of(9L)), eq(RoomStatus.OCCUPIED)))
                .thenReturn(1L);
        when(roomRepository.countByHouseIdInAndStatus(eq(List.of(9L)), eq(RoomStatus.AVAILABLE)))
                .thenReturn(1L);
        when(roomRepository.countByHouseIdInAndStatus(eq(List.of(9L)), eq(RoomStatus.MAINTENANCE)))
                .thenReturn(0L);
        when(roomRepository.findByHouseIdIn(List.of(8L, 9L)))
                .thenReturn(List.of(room(10L, 8L), room(11L, 9L)));
    }

    private static RentalContract contract(Long id, Long roomId) {
        return RentalContract.builder().id(id).contractNo("C-" + id).roomId(roomId).build();
    }

    private static Room room(Long id, Long houseId) {
        Room room = new Room();
        room.setId(id);
        room.setHouseId(houseId);
        room.setRoomNumber("R" + id);
        return room;
    }

    private static Invoice invoice(Long id, InvoiceStatus status, LocalDateTime createdAt) {
        return invoice(id, status, createdAt, null, null);
    }

    private static Invoice invoice(Long id, InvoiceStatus status, LocalDateTime createdAt,
            LocalDate dueDate, String totalAmount) {
        return Invoice.builder().id(id).invoiceNo("INV-" + id).status(status).createdAt(createdAt)
                .dueDate(dueDate)
                .totalAmount(totalAmount == null ? null : new BigDecimal(totalAmount))
                .build();
    }

    private static Payment payment(Long id, Long invoiceId, String amount, PaymentStatus status,
            LocalDateTime paidAt) {
        return Payment.builder().id(id).invoiceId(invoiceId).paymentNo("PAY-" + id)
                .amount(new BigDecimal(amount)).status(status).paidAt(paidAt).build();
    }

    private static MaintenanceRequest maintenanceRequest(Long id, MaintenanceRequestStatus status,
            LocalDateTime reportedAt) {
        return MaintenanceRequest.builder().id(id).requestNo("MR-" + id)
                .status(status).reportedAt(reportedAt).build();
    }
}