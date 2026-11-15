package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.TenantDashboardResponse;
import com.example.spring_boot_project_api.dto.response.TenantInvoiceDetailResponse;
import com.example.spring_boot_project_api.dto.response.TenantInvoiceResponse;
import com.example.spring_boot_project_api.dto.response.TenantPaymentHistoryResponse;
import com.example.spring_boot_project_api.dto.response.TenantUtilityTrendResponse;
import com.example.spring_boot_project_api.enums.InvoiceItemType;
import com.example.spring_boot_project_api.enums.InvoiceStatus;
import com.example.spring_boot_project_api.enums.PaymentStatus;
import com.example.spring_boot_project_api.enums.RentalContractStatus;
import com.example.spring_boot_project_api.enums.UtilityMeterStatus;
import com.example.spring_boot_project_api.exception.InvalidTenantQueryException;
import com.example.spring_boot_project_api.exception.InvoiceNotFoundException;
import com.example.spring_boot_project_api.exception.TenantNotFoundException;
import com.example.spring_boot_project_api.mapper.TenantDashboardMapper;
import com.example.spring_boot_project_api.model.ContractTenant;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.Invoice;
import com.example.spring_boot_project_api.model.InvoiceItem;
import com.example.spring_boot_project_api.model.Payment;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UtilityMeter;
import com.example.spring_boot_project_api.model.UtilityReading;
import com.example.spring_boot_project_api.model.UtilityType;
import com.example.spring_boot_project_api.repository.ContractTenantRepository;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.InvoiceRepository;
import com.example.spring_boot_project_api.repository.PaymentRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.UtilityMeterRepository;
import com.example.spring_boot_project_api.repository.UtilityReadingRepository;
import com.example.spring_boot_project_api.repository.UtilityTypeRepository;
import com.example.spring_boot_project_api.service.impl.TenantContextResolver;
import com.example.spring_boot_project_api.service.impl.TenantDashboardServiceImpl;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantDashboardServiceImplTest {

    @Mock private TenantContextResolver tenantContextResolver;
    @Mock private SecurityUtils securityUtils;
    @Mock private ContractTenantRepository contractTenantRepository;
    @Mock private RentalContractRepository rentalContractRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private HouseRepository houseRepository;
    @Mock private UtilityMeterRepository utilityMeterRepository;
    @Mock private UtilityReadingRepository utilityReadingRepository;
    @Mock private UtilityTypeRepository utilityTypeRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private TenantDashboardMapper tenantDashboardMapper;

    @InjectMocks private TenantDashboardServiceImpl service;

    private User tenantUser;
    private Tenant tenant;
    private ContractTenant link;

    @BeforeEach
    void setUp() {
        tenantUser = User.builder().id(1L).username("sopha").build();

        tenant = Tenant.builder()
                .id(50L)
                .tenantCode("TN-A01")
                .firstName("Sopha")
                .lastName("Chan")
                .build();

        link = ContractTenant.builder().id(1L).tenantId(50L).contractId(100L).build();

        when(securityUtils.hasRole(SecurityUtils.ROLE_TENANT)).thenReturn(true);
        when(securityUtils.getCurrentUser()).thenReturn(tenantUser);
        when(tenantContextResolver.requireLinkedTenant(1L)).thenReturn(tenant);
        when(contractTenantRepository.findByTenantId(50L)).thenReturn(List.of(link));
    }

    private RentalContract activeContract() {
        return RentalContract.builder()
                .id(100L)
                .contractNo("C-001")
                .roomId(10L)
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2027, 1, 1))
                .monthlyRent(new BigDecimal("150000"))
                .dueDay(5)
                .status(RentalContractStatus.ACTIVE)
                .build();
    }

    private Room room() {
        Room room = new Room();
        room.setId(10L);
        room.setHouseId(8L);
        room.setRoomNumber("A101");
        room.setName("Room 101");
        return room;
    }

    private House house() {
        return House.builder().id(8L).name("Golden House").build();
    }

    private Invoice invoice(Long id, Integer year, Integer month, String total, InvoiceStatus status,
            LocalDate dueDate) {
        return Invoice.builder()
                .id(id)
                .invoiceNo("INV-" + year + "-" + String.format("%04d", id))
                .contractId(100L)
                .billingYear(year)
                .billingMonth(month)
                .subtotal(new BigDecimal(total))
                .discount(BigDecimal.ZERO)
                .lateFee(BigDecimal.ZERO)
                .totalAmount(new BigDecimal(total))
                .status(status)
                .dueDate(dueDate)
                .build();
    }

    @Test
    void dashboard_requiresTenantRole() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_TENANT)).thenReturn(false);

        assertThatThrownBy(() -> service.getDashboard())
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void dashboard_requiresLinkedTenant() {
        when(tenantContextResolver.requireLinkedTenant(1L))
                .thenThrow(new TenantNotFoundException("No tenant profile is linked to your account."));

        assertThatThrownBy(() -> service.getDashboard())
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void dashboard_withoutActiveContractReturnsSafeEmptyState() {
        when(tenantContextResolver.activeContractOf(50L)).thenReturn(null);
        when(invoiceRepository.sumTotalByContractIdInAndStatus(List.of(100L), InvoiceStatus.PAID))
                .thenReturn(BigDecimal.ZERO);

        TenantDashboardResponse response = service.getDashboard();

        assertThat(response.tenant().id()).isEqualTo(50L);
        assertThat(response.tenant().name()).isEqualTo("Sopha Chan");
        assertThat(response.room()).isNull();
        assertThat(response.contract()).isNull();
        assertThat(response.financial().currentDue()).isEqualByComparingTo("0");
        assertThat(response.financial().totalPaid()).isEqualByComparingTo("0");
        assertThat(response.financial().unpaidInvoices()).isZero();
        assertThat(response.financial().paidInvoices()).isZero();
        assertThat(response.utilities().water()).isNull();
        assertThat(response.utilities().electricity()).isNull();
    }

    @Test
    void dashboard_withActiveContractAndUnpaidInvoicesComputesFinancials() {
        RentalContract contract = activeContract();
        when(tenantContextResolver.activeContractOf(50L)).thenReturn(contract);
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room()));
        when(houseRepository.findById(8L)).thenReturn(Optional.of(house()));

        Invoice unpaid = invoice(10L, 2026, 9, "100.00", InvoiceStatus.PENDING,
                LocalDate.now().plusDays(30));
        when(invoiceRepository.findByContractIdInAndStatusIn(eq(List.of(100L)), anyList()))
                .thenReturn(List.of(unpaid));
        when(invoiceRepository.countByContractIdInAndStatus(List.of(100L), InvoiceStatus.PAID))
                .thenReturn(3L);
        when(invoiceRepository.sumTotalByContractIdInAndStatus(List.of(100L), InvoiceStatus.PAID))
                .thenReturn(new BigDecimal("450.00"));
        when(paymentRepository.sumPaidAmountByInvoiceIds(List.of(10L), PaymentStatus.PAID))
                .thenReturn(List.of());

        TenantDashboardResponse response = service.getDashboard();

        assertThat(response.room().roomNumber()).isEqualTo("A101");
        assertThat(response.contract().contractNumber()).isEqualTo("C-001");
        assertThat(response.financial().currentDue()).isEqualByComparingTo("100.00");
        assertThat(response.financial().totalPaid()).isEqualByComparingTo("450.00");
        assertThat(response.financial().unpaidInvoices()).isEqualTo(1);
        assertThat(response.financial().overdueInvoices()).isZero();
        assertThat(response.financial().paidInvoices()).isEqualTo(3);
    }

    @Test
    void dashboard_currentDueReducesByPaidAmount() {
        when(tenantContextResolver.activeContractOf(50L)).thenReturn(activeContract());
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room()));
        when(houseRepository.findById(8L)).thenReturn(Optional.of(house()));

        Invoice unpaid = invoice(10L, 2026, 9, "100.00", InvoiceStatus.PARTIAL,
                LocalDate.now().plusDays(30));
        when(invoiceRepository.findByContractIdInAndStatusIn(eq(List.of(100L)), anyList()))
                .thenReturn(List.of(unpaid));
        when(invoiceRepository.countByContractIdInAndStatus(List.of(100L), InvoiceStatus.PAID))
                .thenReturn(0L);
        when(paymentRepository.sumPaidAmountByInvoiceIds(List.of(10L), PaymentStatus.PAID))
                .thenReturn(List.<Object[]>of(new Object[]{10L, new BigDecimal("40.00")}));

        TenantDashboardResponse response = service.getDashboard();

        assertThat(response.financial().currentDue()).isEqualByComparingTo("60.00");
        assertThat(response.financial().overdueInvoices()).isZero();
    }

    @Test
    void dashboard_latestUtilityUsageFromActiveMeters() {
        when(tenantContextResolver.activeContractOf(50L)).thenReturn(activeContract());
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room()));

        User owner = User.builder().id(2L).build();
        UtilityType electricity = UtilityType.builder().id(5L).code("ELECTRICITY").unit("kWh").build();
        UtilityMeter meter = UtilityMeter.builder()
                .id(20L).owner(owner).roomId(10L).utilityTypeId(5L)
                .meterNumber("M-001").status(UtilityMeterStatus.ACTIVE).build();
        UtilityReading reading = UtilityReading.builder()
                .id(30L).meter(meter).usage(new BigDecimal("75.5"))
                .readingDate(LocalDate.of(2026, 9, 1)).build();

        when(utilityMeterRepository.findByRoomId(10L)).thenReturn(List.of(meter));
        when(utilityTypeRepository.findById(5L)).thenReturn(Optional.of(electricity));
        when(utilityReadingRepository.findTopByMeterIdOrderByReadingDateDesc(20L))
                .thenReturn(Optional.of(reading));

        TenantDashboardResponse response = service.getDashboard();

        assertThat(response.utilities().electricity().usage()).isEqualByComparingTo("75.5");
        assertThat(response.utilities().electricity().unit()).isEqualTo("kWh");
        assertThat(response.utilities().electricity().month()).isEqualTo("2026-09");
        assertThat(response.utilities().water()).isNull();
    }

    @Test
    void utilityUsage_returnsMonthlyTrendsAscending() {
        when(tenantContextResolver.activeContractOf(50L)).thenReturn(activeContract());
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room()));

        User owner = User.builder().id(2L).build();
        UtilityType electricity = UtilityType.builder().id(5L).code("ELECTRICITY").unit("kWh").build();
        UtilityType water = UtilityType.builder().id(6L).code("WATER").unit("m3").build();
        UtilityMeter elecMeter = UtilityMeter.builder()
                .id(20L).owner(owner).roomId(10L).utilityTypeId(5L).meterNumber("M-001")
                .status(UtilityMeterStatus.ACTIVE).build();
        UtilityMeter waterMeter = UtilityMeter.builder()
                .id(21L).owner(owner).roomId(10L).utilityTypeId(6L).meterNumber("M-002")
                .status(UtilityMeterStatus.ACTIVE).build();

        when(utilityMeterRepository.findByRoomId(10L)).thenReturn(List.of(elecMeter, waterMeter));
        when(utilityTypeRepository.findById(5L)).thenReturn(Optional.of(electricity));
        when(utilityTypeRepository.findById(6L)).thenReturn(Optional.of(water));

        UtilityReading elecAug = readingOf(elecMeter, "100", LocalDate.of(2026, 8, 31));
        UtilityReading elecSep = readingOf(elecMeter, "75", LocalDate.of(2026, 9, 30));
        UtilityReading waterSep = readingOf(waterMeter, "15", LocalDate.of(2026, 9, 30));
        when(utilityReadingRepository.findByMeterIdInAndReadingDateBetween(
                List.of(20L, 21L), LocalDate.of(2026, 4, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(List.of(elecAug, elecSep, waterSep));

        TenantUtilityTrendResponse response =
                service.getUtilityUsage(YearMonth.of(2026, 4), YearMonth.of(2026, 9));

        assertThat(response.water()).hasSize(1);
        assertThat(response.water().get(0).usage()).isEqualByComparingTo("15");
        assertThat(response.water().get(0).month()).isEqualTo("2026-09");
        assertThat(response.electricity()).hasSize(2);
        assertThat(response.electricity().get(0).month()).isEqualTo("2026-08");
        assertThat(response.electricity().get(0).usage()).isEqualByComparingTo("100");
        assertThat(response.electricity().get(1).month()).isEqualTo("2026-09");
        assertThat(response.electricity().get(1).usage()).isEqualByComparingTo("75");
    }

    @Test
    void utilityUsage_recognizesElecAndWaterTypeCodes() {
        when(tenantContextResolver.activeContractOf(50L)).thenReturn(activeContract());
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room()));

        User owner = User.builder().id(2L).build();
        UtilityType electricity = UtilityType.builder().id(5L).code("ELEC").unit("kWh").build();
        UtilityType water = UtilityType.builder().id(6L).code("water").unit("m3").build();
        UtilityMeter elecMeter = UtilityMeter.builder()
                .id(20L).owner(owner).roomId(10L).utilityTypeId(5L).meterNumber("M-001")
                .status(UtilityMeterStatus.ACTIVE).build();
        UtilityMeter waterMeter = UtilityMeter.builder()
                .id(21L).owner(owner).roomId(10L).utilityTypeId(6L).meterNumber("M-002")
                .status(UtilityMeterStatus.ACTIVE).build();

        when(utilityMeterRepository.findByRoomId(10L)).thenReturn(List.of(elecMeter, waterMeter));
        when(utilityTypeRepository.findById(5L)).thenReturn(Optional.of(electricity));
        when(utilityTypeRepository.findById(6L)).thenReturn(Optional.of(water));
        when(utilityReadingRepository.findByMeterIdInAndReadingDateBetween(
                List.of(20L, 21L), LocalDate.of(2026, 4, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(List.of(
                        readingOf(elecMeter, "21", LocalDate.of(2026, 9, 30)),
                        readingOf(waterMeter, "11", LocalDate.of(2026, 9, 30))));

        TenantUtilityTrendResponse response =
                service.getUtilityUsage(YearMonth.of(2026, 4), YearMonth.of(2026, 9));

        assertThat(response.electricity()).hasSize(1);
        assertThat(response.electricity().get(0).unit()).isEqualTo("kWh");
        assertThat(response.electricity().get(0).usage()).isEqualByComparingTo("21");
        assertThat(response.water()).hasSize(1);
        assertThat(response.water().get(0).unit()).isEqualTo("m3");
        assertThat(response.water().get(0).usage()).isEqualByComparingTo("11");
    }

    @Test
    void utilityUsage_withoutActiveContractReturnsEmpty() {
        when(tenantContextResolver.activeContractOf(50L)).thenReturn(null);

        TenantUtilityTrendResponse response =
                service.getUtilityUsage(YearMonth.of(2026, 4), YearMonth.of(2026, 9));

        assertThat(response.water()).isEmpty();
        assertThat(response.electricity()).isEmpty();
    }

    @Test
    void utilityUsage_rejectsInvertedRange() {
        assertThatThrownBy(() -> service.getUtilityUsage(YearMonth.of(2026, 9), YearMonth.of(2026, 4)))
                .isInstanceOf(InvalidTenantQueryException.class);
    }

    @Test
    void paymentHistory_aggregatesMonthsWithCorrectStatus() {
        Invoice paid = invoice(1L, 2026, 6, "90.00", InvoiceStatus.PAID,
                LocalDate.of(2026, 6, 5));
        Invoice overdue = invoice(2L, 2026, 7, "85.00", InvoiceStatus.OVERDUE,
                LocalDate.of(2026, 7, 5));
        Invoice partial = invoice(3L, 2026, 8, "95.00", InvoiceStatus.PARTIAL,
                LocalDate.of(2026, 8, 5));

        when(invoiceRepository.findByContractIdIn(List.of(100L))).thenReturn(List.of(paid, overdue, partial));
        when(paymentRepository.findByInvoiceIdInAndStatus(
                List.of(1L, 2L, 3L), PaymentStatus.PAID))
                .thenReturn(List.of(
                        payment(1L, "90.00", LocalDate.of(2026, 6, 5)),
                        payment(3L, "40.00", LocalDate.of(2026, 8, 1))));

        List<TenantPaymentHistoryResponse> history =
                service.getPaymentHistory(YearMonth.of(2026, 6), YearMonth.of(2026, 8));

        assertThat(history).hasSize(3);
        assertThat(history.get(0).month()).isEqualTo("2026-06");
        assertThat(history.get(0).status()).isEqualTo(InvoiceStatus.PAID);
        assertThat(history.get(1).month()).isEqualTo("2026-07");
        assertThat(history.get(1).status()).isEqualTo(InvoiceStatus.OVERDUE);
        assertThat(history.get(2).month()).isEqualTo("2026-08");
        assertThat(history.get(2).status()).isEqualTo(InvoiceStatus.PARTIAL);
        assertThat(history.get(2).amount()).isEqualByComparingTo("95.00");
    }

    @Test
    void paymentHistory_excludesInvoicesOutsideRange() {
        Invoice outside = invoice(1L, 2026, 1, "90.00", InvoiceStatus.PAID,
                LocalDate.of(2026, 1, 5));
        Invoice inside = invoice(2L, 2026, 7, "85.00", InvoiceStatus.PAID,
                LocalDate.of(2026, 7, 5));
        when(invoiceRepository.findByContractIdIn(List.of(100L))).thenReturn(List.of(outside, inside));
        when(paymentRepository.findByInvoiceIdInAndStatus(
                List.of(1L, 2L), PaymentStatus.PAID))
                .thenReturn(List.of(payment(2L, "85.00", LocalDate.of(2026, 7, 5))));

        List<TenantPaymentHistoryResponse> history =
                service.getPaymentHistory(YearMonth.of(2026, 6), YearMonth.of(2026, 8));

        assertThat(history).hasSize(1);
        assertThat(history.get(0).month()).isEqualTo("2026-07");
        assertThat(history.get(0).status()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void dashboard_utilitySummaryRecognizesUtilitiesByUnitForPlaceholderCodes() {
        when(tenantContextResolver.activeContractOf(50L)).thenReturn(activeContract());
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room()));

        User owner = User.builder().id(2L).build();
        UtilityType electricity = UtilityType.builder().id(5L).code("DEFAULT4876\u17E3").unit("kWh").build();
        UtilityType water = UtilityType.builder().id(6L).code("DEFAULT4876").unit("m3").build();
        UtilityMeter elecMeter = UtilityMeter.builder()
                .id(20L).owner(owner).roomId(10L).utilityTypeId(5L).meterNumber("M-001")
                .status(UtilityMeterStatus.ACTIVE).build();
        UtilityMeter waterMeter = UtilityMeter.builder()
                .id(21L).owner(owner).roomId(10L).utilityTypeId(6L).meterNumber("M-002")
                .status(UtilityMeterStatus.ACTIVE).build();

        when(utilityMeterRepository.findByRoomId(10L)).thenReturn(List.of(elecMeter, waterMeter));
        when(utilityTypeRepository.findById(5L)).thenReturn(Optional.of(electricity));
        when(utilityTypeRepository.findById(6L)).thenReturn(Optional.of(water));
        when(utilityReadingRepository.findTopByMeterIdOrderByReadingDateDesc(20L))
                .thenReturn(Optional.of(readingOf(elecMeter, "11", LocalDate.of(2026, 12, 31))));
        when(utilityReadingRepository.findTopByMeterIdOrderByReadingDateDesc(21L))
                .thenReturn(Optional.of(readingOf(waterMeter, "11", LocalDate.of(2026, 12, 31))));

        TenantDashboardResponse response = service.getDashboard();

        assertThat(response.utilities().electricity()).isNotNull();
        assertThat(response.utilities().electricity().usage()).isEqualByComparingTo("11");
        assertThat(response.utilities().electricity().unit()).isEqualTo("kWh");
        assertThat(response.utilities().electricity().month()).isEqualTo("2026-12");
        assertThat(response.utilities().water()).isNotNull();
        assertThat(response.utilities().water().usage()).isEqualByComparingTo("11");
        assertThat(response.utilities().water().unit()).isEqualTo("m3");
        assertThat(response.utilities().water().month()).isEqualTo("2026-12");
    }

    @Test
    void utilityUsage_recognizesBucketsByUnitWhenCodeIsUnrecognized() {
        when(tenantContextResolver.activeContractOf(50L)).thenReturn(activeContract());
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room()));

        User owner = User.builder().id(2L).build();
        UtilityType electricity = UtilityType.builder().id(5L).code("DEFAULT4876\u17E3").unit("kWh").build();
        UtilityType water = UtilityType.builder().id(6L).code("DEFAULT4876").unit("m3").build();
        UtilityMeter elecMeter = UtilityMeter.builder()
                .id(20L).owner(owner).roomId(10L).utilityTypeId(5L).meterNumber("M-001")
                .status(UtilityMeterStatus.ACTIVE).build();
        UtilityMeter waterMeter = UtilityMeter.builder()
                .id(21L).owner(owner).roomId(10L).utilityTypeId(6L).meterNumber("M-002")
                .status(UtilityMeterStatus.ACTIVE).build();

        when(utilityMeterRepository.findByRoomId(10L)).thenReturn(List.of(elecMeter, waterMeter));
        when(utilityTypeRepository.findById(5L)).thenReturn(Optional.of(electricity));
        when(utilityTypeRepository.findById(6L)).thenReturn(Optional.of(water));
        when(utilityReadingRepository.findByMeterIdInAndReadingDateBetween(
                List.of(20L, 21L), LocalDate.of(2026, 4, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(List.of(
                        readingOf(elecMeter, "21", LocalDate.of(2026, 9, 30)),
                        readingOf(waterMeter, "11", LocalDate.of(2026, 9, 30))));

        TenantUtilityTrendResponse response =
                service.getUtilityUsage(YearMonth.of(2026, 4), YearMonth.of(2026, 9));

        assertThat(response.electricity()).hasSize(1);
        assertThat(response.electricity().get(0).usage()).isEqualByComparingTo("21");
        assertThat(response.electricity().get(0).unit()).isEqualTo("kWh");
        assertThat(response.water()).hasSize(1);
        assertThat(response.water().get(0).usage()).isEqualByComparingTo("11");
        assertThat(response.water().get(0).unit()).isEqualTo("m3");
    }

    @Test
    void dashboard_ignoresInactiveMeters() {
        when(tenantContextResolver.activeContractOf(50L)).thenReturn(activeContract());
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room()));

        User owner = User.builder().id(2L).build();
        UtilityType electricity = UtilityType.builder().id(5L).code("ELEC").unit("kWh").build();
        UtilityType water = UtilityType.builder().id(6L).code("WTR").unit("m3").build();
        UtilityMeter elecMeter = UtilityMeter.builder()
                .id(20L).owner(owner).roomId(10L).utilityTypeId(5L).meterNumber("M-001")
                .status(UtilityMeterStatus.ACTIVE).build();
        UtilityMeter waterMeter = UtilityMeter.builder()
                .id(21L).owner(owner).roomId(10L).utilityTypeId(6L).meterNumber("M-002")
                .status(UtilityMeterStatus.INACTIVE).build();

        when(utilityMeterRepository.findByRoomId(10L)).thenReturn(List.of(elecMeter, waterMeter));
        when(utilityTypeRepository.findById(5L)).thenReturn(Optional.of(electricity));
        when(utilityTypeRepository.findById(6L)).thenReturn(Optional.of(water));
        when(utilityReadingRepository.findTopByMeterIdOrderByReadingDateDesc(20L))
                .thenReturn(Optional.of(readingOf(elecMeter, "75.5", LocalDate.of(2026, 9, 30))));

        TenantDashboardResponse response = service.getDashboard();

        assertThat(response.utilities().electricity()).isNotNull();
        assertThat(response.utilities().electricity().usage()).isEqualByComparingTo("75.5");
        assertThat(response.utilities().water()).isNull();
    }

    @Test
    void dashboard_skipsUtilityTypesWithoutRecognizedBucket() {
        when(tenantContextResolver.activeContractOf(50L)).thenReturn(activeContract());
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room()));

        User owner = User.builder().id(2L).build();
        UtilityType garbage = UtilityType.builder().id(7L).code("TRASH").unit("month").build();
        UtilityMeter meter = UtilityMeter.builder()
                .id(20L).owner(owner).roomId(10L).utilityTypeId(7L).meterNumber("M-003")
                .status(UtilityMeterStatus.ACTIVE).build();

        when(utilityMeterRepository.findByRoomId(10L)).thenReturn(List.of(meter));
        when(utilityTypeRepository.findById(7L)).thenReturn(Optional.of(garbage));
        when(utilityReadingRepository.findTopByMeterIdOrderByReadingDateDesc(20L))
                .thenReturn(Optional.of(readingOf(meter, "5", LocalDate.of(2026, 9, 30))));

        TenantDashboardResponse response = service.getDashboard();

        assertThat(response.utilities().water()).isNull();
        assertThat(response.utilities().electricity()).isNull();
    }

    @Test
    void dashboard_keepsUtilitiesEmptyWhenMetersHaveNoReadings() {
        when(tenantContextResolver.activeContractOf(50L)).thenReturn(activeContract());
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room()));

        User owner = User.builder().id(2L).build();
        UtilityType electricity = UtilityType.builder().id(5L).code("ELECTRICITY").unit("kWh").build();
        UtilityMeter elecMeter = UtilityMeter.builder()
                .id(20L).owner(owner).roomId(10L).utilityTypeId(5L).meterNumber("M-001")
                .status(UtilityMeterStatus.ACTIVE).build();

        when(utilityMeterRepository.findByRoomId(10L)).thenReturn(List.of(elecMeter));
        when(utilityTypeRepository.findById(5L)).thenReturn(Optional.of(electricity));
        when(utilityReadingRepository.findTopByMeterIdOrderByReadingDateDesc(20L))
                .thenReturn(Optional.empty());

        TenantDashboardResponse response = service.getDashboard();

        assertThat(response.utilities().water()).isNull();
        assertThat(response.utilities().electricity()).isNull();
    }

    @Test
    void paymentHistory_surfacesPaidInvoiceWithFutureBillingMonthWhenPaymentIsInRange() {
        Invoice dec = invoice(9L, 2026, 12, "153.52", InvoiceStatus.PAID,
                LocalDate.of(2026, 12, 5));
        when(invoiceRepository.findByContractIdIn(List.of(100L))).thenReturn(List.of(dec));
        when(paymentRepository.findByInvoiceIdInAndStatus(List.of(9L), PaymentStatus.PAID))
                .thenReturn(List.of(payment(9L, "153.52", LocalDate.of(2026, 9, 23))));

        List<TenantPaymentHistoryResponse> history =
                service.getPaymentHistory(YearMonth.of(2026, 1), YearMonth.of(2026, 9));

        assertThat(history).hasSize(1);
        assertThat(history.get(0).month()).isEqualTo("2026-12");
        assertThat(history.get(0).amount()).isEqualByComparingTo("153.52");
        assertThat(history.get(0).status()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void paymentHistory_aggregatesMultiplePaidMonthsAscending() {
        Invoice jun = invoice(1L, 2026, 6, "90.00", InvoiceStatus.PAID,
                LocalDate.of(2026, 6, 5));
        Invoice jul = invoice(2L, 2026, 7, "85.00", InvoiceStatus.PAID,
                LocalDate.of(2026, 7, 5));
        when(invoiceRepository.findByContractIdIn(List.of(100L))).thenReturn(List.of(jun, jul));
        when(paymentRepository.findByInvoiceIdInAndStatus(List.of(1L, 2L), PaymentStatus.PAID))
                .thenReturn(List.of(
                        payment(1L, "90.00", LocalDate.of(2026, 6, 5)),
                        payment(2L, "85.00", LocalDate.of(2026, 7, 5))));

        List<TenantPaymentHistoryResponse> history =
                service.getPaymentHistory(YearMonth.of(2026, 5), YearMonth.of(2026, 8));

        assertThat(history).hasSize(2);
        assertThat(history.get(0).month()).isEqualTo("2026-06");
        assertThat(history.get(0).amount()).isEqualByComparingTo("90.00");
        assertThat(history.get(0).status()).isEqualTo(InvoiceStatus.PAID);
        assertThat(history.get(1).month()).isEqualTo("2026-07");
        assertThat(history.get(1).amount()).isEqualByComparingTo("85.00");
        assertThat(history.get(1).status()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void paymentHistory_isScopedToOwningTenantsContracts() {
        ContractTenant secondLink = ContractTenant.builder().id(2L).tenantId(50L).contractId(200L).build();
        when(contractTenantRepository.findByTenantId(50L)).thenReturn(List.of(link, secondLink));

        Invoice a = invoice(1L, 2026, 6, "90.00", InvoiceStatus.PAID,
                LocalDate.of(2026, 6, 5));
        Invoice b = invoice(2L, 2026, 7, "85.00", InvoiceStatus.PAID,
                LocalDate.of(2026, 7, 5));
        when(invoiceRepository.findByContractIdIn(List.of(100L, 200L))).thenReturn(List.of(a, b));
        when(paymentRepository.findByInvoiceIdInAndStatus(List.of(1L, 2L), PaymentStatus.PAID))
                .thenReturn(List.of(
                        payment(1L, "90.00", LocalDate.of(2026, 6, 5)),
                        payment(2L, "85.00", LocalDate.of(2026, 7, 5))));

        List<TenantPaymentHistoryResponse> history =
                service.getPaymentHistory(YearMonth.of(2026, 6), YearMonth.of(2026, 8));

        verify(invoiceRepository).findByContractIdIn(List.of(100L, 200L));
        assertThat(history).hasSize(2);
        assertThat(history.get(0).month()).isEqualTo("2026-06");
        assertThat(history.get(1).month()).isEqualTo("2026-07");
    }

    @Test
    void paymentHistory_surfacesExistingPaidInvoiceWithoutChangingItsAmount() {
        Invoice jan = invoice(5L, 2026, 1, "120.66", InvoiceStatus.PAID,
                LocalDate.of(2026, 1, 5));
        when(invoiceRepository.findByContractIdIn(List.of(100L))).thenReturn(List.of(jan));
        when(paymentRepository.findByInvoiceIdInAndStatus(List.of(5L), PaymentStatus.PAID))
                .thenReturn(List.of(payment(5L, "120.66", LocalDate.of(2026, 1, 3))));

        List<TenantPaymentHistoryResponse> history =
                service.getPaymentHistory(YearMonth.of(2026, 1), YearMonth.of(2026, 3));

        assertThat(history).hasSize(1);
        assertThat(history.get(0).month()).isEqualTo("2026-01");
        assertThat(history.get(0).amount()).isEqualByComparingTo("120.66");
        assertThat(history.get(0).status()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void paymentHistory_isEmptyOnlyWhenTenantGenuinelyHasNoPaymentData() {
        when(contractTenantRepository.findByTenantId(50L)).thenReturn(List.of());
        assertThat(service.getPaymentHistory(YearMonth.of(2026, 1), YearMonth.of(2026, 9)))
                .isEmpty();

        when(contractTenantRepository.findByTenantId(50L)).thenReturn(List.of(link));
        Invoice pending = invoice(6L, 2026, 9, "80.00", InvoiceStatus.PENDING,
                LocalDate.now().plusDays(30));
        when(invoiceRepository.findByContractIdIn(List.of(100L))).thenReturn(List.of(pending));
        when(paymentRepository.findByInvoiceIdInAndStatus(List.of(6L), PaymentStatus.PAID))
                .thenReturn(List.of());

        List<TenantPaymentHistoryResponse> withUnpaidInvoices =
                service.getPaymentHistory(YearMonth.of(2026, 1), YearMonth.of(2026, 9));

        assertThat(withUnpaidInvoices).hasSize(1);
        assertThat(withUnpaidInvoices.get(0).month()).isEqualTo("2026-09");
        assertThat(withUnpaidInvoices.get(0).status()).isEqualTo(InvoiceStatus.PENDING);
    }

    @Test
    void invoices_returnsPagedTenantInvoices() throws Exception {
        Invoice invoice = invoice(10L, 2026, 8, "85.00", InvoiceStatus.PENDING,
                LocalDate.of(2026, 8, 5));
        Page<Invoice> page = new PageImpl<>(List.of(invoice), PageRequest.of(0, 10), 1);
        when(invoiceRepository.findByContractIdIn(eq(List.of(100L)), any(Pageable.class))).thenReturn(page);
        when(tenantDashboardMapper.toListResponse(invoice))
                .thenReturn(new TenantInvoiceResponse(10L, "INV-2026-0010", "2026-08",
                        new BigDecimal("60"), new BigDecimal("5"), new BigDecimal("20"),
                        new BigDecimal("0"), new BigDecimal("85"), InvoiceStatus.PENDING,
                        LocalDate.of(2026, 8, 5)));

        PageResponse<TenantInvoiceResponse> result = service.getMyInvoices(0, 10);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content().get(0).invoiceNo()).isEqualTo("INV-2026-0010");
        assertThat(result.content().get(0).total()).isEqualByComparingTo("85");
    }

    @Test
    void invoices_rejectsInvalidPagination() {
        assertThatThrownBy(() -> service.getMyInvoices(-1, 10))
                .isInstanceOf(InvalidTenantQueryException.class);
        assertThatThrownBy(() -> service.getMyInvoices(0, 0))
                .isInstanceOf(InvalidTenantQueryException.class);
        assertThatThrownBy(() -> service.getMyInvoices(0, 101))
                .isInstanceOf(InvalidTenantQueryException.class);
    }

    @Test
    void invoiceDetail_throwsForInvoiceNotOwnedByTenant() {
        when(invoiceRepository.findByIdAndContractIdIn(999L, List.of(100L)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyInvoice(999L))
                .isInstanceOf(InvoiceNotFoundException.class);
    }

    @Test
    void invoiceDetail_returnsOwnInvoiceWithUtilityBreakdown() {
        Invoice invoice = invoice(10L, 2026, 8, "90.00", InvoiceStatus.PENDING,
                LocalDate.of(2026, 8, 5));
        invoice.getItems().add(item(invoice, InvoiceItemType.RENT, "Rent", BigDecimal.ONE,
                new BigDecimal("60"), new BigDecimal("60")));
        invoice.getItems().add(item(invoice, InvoiceItemType.WATER, "Water", new BigDecimal("5"),
                new BigDecimal("1"), new BigDecimal("5")));
        invoice.getItems().add(item(invoice, InvoiceItemType.ELECTRICITY, "Electricity",
                new BigDecimal("20"), new BigDecimal("1"), new BigDecimal("20")));
        invoice.getItems().add(item(invoice, InvoiceItemType.INTERNET, "Internet", BigDecimal.ONE,
                new BigDecimal("5"), new BigDecimal("5")));

        RentalContract contract = activeContract();
        when(rentalContractRepository.findById(100L)).thenReturn(Optional.of(contract));
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room()));
        when(houseRepository.findById(8L)).thenReturn(Optional.of(house()));

        when(invoiceRepository.findByIdAndContractIdIn(10L, List.of(100L)))
                .thenReturn(Optional.of(invoice));

        User owner = User.builder().id(2L).build();
        UtilityType electricity = UtilityType.builder().id(5L).code("ELECTRICITY").unit("kWh").build();
        UtilityType water = UtilityType.builder().id(6L).code("WATER").unit("m3").build();
        UtilityMeter elecMeter = UtilityMeter.builder()
                .id(20L).owner(owner).roomId(10L).utilityTypeId(5L).meterNumber("M-001")
                .status(UtilityMeterStatus.ACTIVE).build();
        UtilityMeter waterMeter = UtilityMeter.builder()
                .id(21L).owner(owner).roomId(10L).utilityTypeId(6L).meterNumber("M-002")
                .status(UtilityMeterStatus.ACTIVE).build();
        when(utilityMeterRepository.findByRoomId(10L)).thenReturn(List.of(elecMeter, waterMeter));
        when(utilityTypeRepository.findById(5L)).thenReturn(Optional.of(electricity));
        when(utilityTypeRepository.findById(6L)).thenReturn(Optional.of(water));

        TenantInvoiceDetailResponse expected = new TenantInvoiceDetailResponse(
                10L, "INV-2026-0010", "2026-08", "Golden House", "A101",
                new BigDecimal("60"), List.of(), new BigDecimal("5"),
                new BigDecimal("90.00"), InvoiceStatus.PENDING, LocalDate.of(2026, 8, 5));
        when(tenantDashboardMapper.toDetailResponse(any(), any(), any(), any())).thenReturn(expected);

        TenantInvoiceDetailResponse response = service.getMyInvoice(10L);

        assertThat(response).isSameAs(expected);
        assertThat(response.roomNumber()).isEqualTo("A101");
        assertThat(response.total()).isEqualByComparingTo("90.00");
    }

    private static Payment payment(Long invoiceId, String amount, LocalDate paidDate) {
        return Payment.builder()
                .id(invoiceId)
                .invoiceId(invoiceId)
                .paymentNo("PAY-" + invoiceId)
                .transactionId("TXN-" + invoiceId)
                .amount(new BigDecimal(amount))
                .status(PaymentStatus.PAID)
                .paidAt(paidDate.atStartOfDay())
                .createdAt(paidDate.atStartOfDay())
                .build();
    }

    private static UtilityReading readingOf(UtilityMeter meter, String usage, LocalDate date) {
        return UtilityReading.builder()
                .meter(meter)
                .usage(new BigDecimal(usage))
                .readingDate(date)
                .build();
    }

    private static InvoiceItem item(Invoice invoice, InvoiceItemType type, String description,
            BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount) {
        InvoiceItem item = InvoiceItem.builder()
                .invoice(invoice)
                .itemType(type)
                .description(description)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .amount(amount)
                .build();
        return item;
    }
}