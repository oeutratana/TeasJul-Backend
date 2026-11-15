package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.spring_boot_project_api.dto.request.BulkGenerateInvoiceRequest;
import com.example.spring_boot_project_api.dto.request.GenerateInvoiceRequest;
import com.example.spring_boot_project_api.dto.response.BulkGenerateInvoiceResponse;
import com.example.spring_boot_project_api.dto.response.ContractEligibleResponse;
import com.example.spring_boot_project_api.dto.response.GenerateBulkPreviewResponse;
import com.example.spring_boot_project_api.dto.response.InvoicePreviewResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.enums.InvoiceItemType;
import com.example.spring_boot_project_api.enums.RentalContractStatus;
import com.example.spring_boot_project_api.enums.UtilityBillingMethod;
import com.example.spring_boot_project_api.enums.UtilityMeterStatus;
import com.example.spring_boot_project_api.enums.UtilityRateStatus;
import com.example.spring_boot_project_api.enums.UtilityTypeStatus;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.InvalidInvoiceOperationException;
import com.example.spring_boot_project_api.mapper.InvoiceMapper;
import com.example.spring_boot_project_api.model.ContractTenant;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.Invoice;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.RoomUtility;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UtilityMeter;
import com.example.spring_boot_project_api.model.UtilityRate;
import com.example.spring_boot_project_api.model.UtilityReading;
import com.example.spring_boot_project_api.model.UtilityType;
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
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InvoiceServiceImplTest {

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

    private User owner;
    private House house;
    private Room room;
    private RentalContract contract;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).username("owner").email("owner@example.com")
                .passwordHash("h").firstName("Owner").lastName("One").build();
        house = House.builder().id(10L).owner(owner).name("Mansion").build();
        room = new Room();
        room.setId(20L);
        room.setOwnerId(1L);
        room.setHouseId(10L);
        room.setRoomNumber("A-1");
        room.setMonthlyRent(new BigDecimal("150.00"));
        contract = RentalContract.builder().id(30L).ownerId(1L).contractNo("CT-30")
                .roomId(20L).startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2027, 1, 1))
                .monthlyRent(new BigDecimal("150.00"))
                .dueDay(5).status(RentalContractStatus.ACTIVE).build();
        when(ownerSettingService.getExchangeRate(any())).thenReturn(new BigDecimal("4100"));
        when(ownerSettingService.getInvoicePrefix(any())).thenReturn("INV");
        when(ownerSettingService.getInvoiceDefaultDueDays(any())).thenReturn(5);
        when(paymentRepository.findPaidTotalByInvoiceId(any())).thenReturn(BigDecimal.ZERO);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authAsOwner() {
        UserDetails details = org.springframework.security.core.userdetails.User.builder()
                .username(owner.getUsername()).password(owner.getPasswordHash()).roles("USER").build();
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(details, "n/a"));
        when(userRepository.findByUsername(owner.getUsername())).thenReturn(Optional.of(owner));
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(true);
        when(securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)).thenReturn(false);
        when(houseAccessService.getAccessibleHouseIds()).thenReturn(List.of(10L));
    }

    private void stubBillingScene() {
        when(contractRepository.findById(30L)).thenReturn(Optional.of(contract));
        when(roomRepository.findById(20L)).thenReturn(Optional.of(room));
        when(houseRepository.findById(10L)).thenReturn(Optional.of(house));
        when(contractTenantRepository.findByContractId(30L)).thenReturn(List.of());
    }

    private UtilityType meteredType(Long id, String code, String name) {
        return UtilityType.builder().id(id).owner(owner).name(name).code(code).unit("m3")
                .billingMethod(UtilityBillingMethod.METERED).status(UtilityTypeStatus.ACTIVE).build();
    }

    private UtilityType fixedType(Long id, String code, String name) {
        return UtilityType.builder().id(id).owner(owner).name(name).code(code).unit("")
                .billingMethod(UtilityBillingMethod.FIXED).status(UtilityTypeStatus.ACTIVE).build();
    }

    private RoomUtility roomUtility(Long typeId) {
        return RoomUtility.builder().id(100L + typeId).owner(owner)
                .roomId(20L).utilityTypeId(typeId).isIncluded(false)
                .status(UtilityTypeStatus.ACTIVE).build();
    }

    private UtilityRate rate(Long typeId, BigDecimal value) {
        return UtilityRate.builder().id(typeId + 500L).owner(owner).houseId(10L)
                .utilityTypeId(typeId).rate(value)
                .effectiveFrom(LocalDate.of(2026, 1, 1))
                .status(UtilityRateStatus.ACTIVE).build();
    }

    @Test
    void previewRentOnly() {
        authAsOwner();
        stubBillingScene();
        when(roomUtilityRepository.findByRoomIdAndStatus(20L, UtilityTypeStatus.ACTIVE))
                .thenReturn(List.of());

        InvoicePreviewResponse resp = invoiceService.previewInvoice(
                new GenerateInvoiceRequest(30L, 2026, 2, null, null));

        assertThat(resp.items()).hasSize(1);
        assertThat(resp.items().get(0).itemType()).isEqualTo(InvoiceItemType.RENT);
        assertThat(resp.items().get(0).amount()).isEqualByComparingTo("150.00");
        assertThat(resp.total()).isEqualByComparingTo("150.00");
    }

    @Test
    void previewMeteredAndFixed() {
        authAsOwner();
        stubBillingScene();
        UtilityType water = meteredType(1L, "WATER", "Water");
        UtilityType parking = fixedType(4L, "PARKING", "Parking");

        when(roomUtilityRepository.findByRoomIdAndStatus(20L, UtilityTypeStatus.ACTIVE))
                .thenReturn(List.of(roomUtility(1L), roomUtility(4L)));
        when(typeRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(water));
        when(typeRepository.findByIdAndOwnerId(4L, 1L)).thenReturn(Optional.of(parking));

        UtilityMeter meter = UtilityMeter.builder().id(90L).owner(owner).houseId(10L).roomId(20L)
                .utilityTypeId(1L).status(UtilityMeterStatus.ACTIVE).build();
        when(meterRepository.findByRoomIdAndUtilityTypeId(20L, 1L)).thenReturn(List.of(meter));

        UtilityReading baseline = UtilityReading.builder().id(1L).meter(meter)
                .readingDate(LocalDate.of(2026, 1, 31))
                .currentReading(new BigDecimal("100")).build();
        UtilityReading current = UtilityReading.builder().id(2L).meter(meter)
                .readingDate(LocalDate.of(2026, 2, 28))
                .currentReading(new BigDecimal("150")).build();
        when(invoiceRepository.findReadingsForPeriod(meter.getId(), LocalDate.of(2026, 1, 31)))
                .thenReturn(List.of(baseline));
        when(invoiceRepository.findReadingsForPeriod(meter.getId(), LocalDate.of(2026, 2, 28)))
                .thenReturn(List.of(current));

        when(invoiceRepository.findEffectiveRate(1L, 10L, 1L, LocalDate.of(2026, 2, 28),
                UtilityRateStatus.ACTIVE)).thenReturn(List.of(rate(1L, new BigDecimal("2.0000"))));
        when(invoiceRepository.findEffectiveRate(1L, 10L, 4L, LocalDate.of(2026, 2, 28),
                UtilityRateStatus.ACTIVE)).thenReturn(List.of(rate(4L, new BigDecimal("20.0000"))));

        InvoicePreviewResponse resp = invoiceService.previewInvoice(
                new GenerateInvoiceRequest(30L, 2026, 2, null, null));

        // rent 150 + water (50 * 2) + parking 20 = 270
        assertThat(resp.items()).hasSize(3);
        assertThat(resp.total()).isEqualByComparingTo("270.00");
        var waterItem = resp.items().stream()
                .filter(i -> i.itemType() == InvoiceItemType.WATER).findFirst().orElseThrow();
        assertThat(waterItem.quantity()).isEqualByComparingTo("50");
        var parkingItem = resp.items().stream()
                .filter(i -> i.itemType() == InvoiceItemType.PARKING).findFirst().orElseThrow();
        assertThat(parkingItem.quantity()).isEqualByComparingTo("1");
        assertThat(parkingItem.amount()).isEqualByComparingTo("20.00");
    }

    @Test
    void meteredUtilityWithoutReadingIsSkipped() {
        authAsOwner();
        stubBillingScene();
        UtilityType water = meteredType(1L, "WATER", "Water");
        when(roomUtilityRepository.findByRoomIdAndStatus(20L, UtilityTypeStatus.ACTIVE))
                .thenReturn(List.of(roomUtility(1L)));
        when(typeRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(water));
        UtilityMeter meter = UtilityMeter.builder().id(90L).owner(owner).houseId(10L).roomId(20L)
                .utilityTypeId(1L).status(UtilityMeterStatus.ACTIVE).build();
        when(meterRepository.findByRoomIdAndUtilityTypeId(20L, 1L)).thenReturn(List.of(meter));
        when(invoiceRepository.findReadingsForPeriod(meter.getId(), LocalDate.of(2026, 1, 31)))
                .thenReturn(List.of());
        when(invoiceRepository.findReadingsForPeriod(meter.getId(), LocalDate.of(2026, 2, 28)))
                .thenReturn(List.of());

        InvoicePreviewResponse resp = invoiceService.previewInvoice(
                new GenerateInvoiceRequest(30L, 2026, 2, null, null));

        // No reading for the period: the metered utility is skipped, never invented.
        assertThat(resp.items()).hasSize(1);
        assertThat(resp.items().get(0).itemType()).isEqualTo(InvoiceItemType.RENT);
        assertThat(resp.total()).isEqualByComparingTo("150.00");
        verify(invoiceRepository, never()).findEffectiveRate(eq(1L), eq(10L), eq(1L),
                any(LocalDate.class), any(UtilityRateStatus.class));
    }

    @Test
    void meteredUtilityWithReadingsIsBilled() {
        authAsOwner();
        stubBillingScene();
        UtilityType water = meteredType(1L, "WATER", "Water");
        when(roomUtilityRepository.findByRoomIdAndStatus(20L, UtilityTypeStatus.ACTIVE))
                .thenReturn(List.of(roomUtility(1L)));
        when(typeRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(water));
        UtilityMeter meter = UtilityMeter.builder().id(90L).owner(owner).houseId(10L).roomId(20L)
                .utilityTypeId(1L).status(UtilityMeterStatus.ACTIVE).build();
        when(meterRepository.findByRoomIdAndUtilityTypeId(20L, 1L)).thenReturn(List.of(meter));
        UtilityReading baseline = UtilityReading.builder().id(1L).meter(meter)
                .readingDate(LocalDate.of(2026, 1, 31))
                .currentReading(new BigDecimal("100")).build();
        UtilityReading current = UtilityReading.builder().id(2L).meter(meter)
                .readingDate(LocalDate.of(2026, 2, 28))
                .currentReading(new BigDecimal("150")).build();
        when(invoiceRepository.findReadingsForPeriod(meter.getId(), LocalDate.of(2026, 1, 31)))
                .thenReturn(List.of(baseline));
        when(invoiceRepository.findReadingsForPeriod(meter.getId(), LocalDate.of(2026, 2, 28)))
                .thenReturn(List.of(current));
        when(invoiceRepository.findEffectiveRate(1L, 10L, 1L, LocalDate.of(2026, 2, 28),
                UtilityRateStatus.ACTIVE)).thenReturn(List.of(rate(1L, new BigDecimal("2.0000"))));

        InvoicePreviewResponse resp = invoiceService.previewInvoice(
                new GenerateInvoiceRequest(30L, 2026, 2, null, null));

        assertThat(resp.items()).hasSize(2);
        var waterItem = resp.items().stream()
                .filter(i -> i.itemType() == InvoiceItemType.WATER).findFirst().orElseThrow();
        // usage 150 - 100 = 50 at 2.0000 -> 100.00
        assertThat(waterItem.quantity()).isEqualByComparingTo("50");
        assertThat(waterItem.unitPrice()).isEqualByComparingTo("2.0000");
        assertThat(waterItem.amount()).isEqualByComparingTo("100.00");
        assertThat(resp.total()).isEqualByComparingTo("250.00");
    }

    @Test
    void rentPlusMeteredElectricity() {
        authAsOwner();
        stubBillingScene();
        UtilityType electricity = meteredType(2L, "ELECTRICITY", "Electricity");
        RoomUtility electricityRu = roomUtility(2L);
        electricityRu.setIsIncluded(true);
        when(roomUtilityRepository.findByRoomIdAndStatus(20L, UtilityTypeStatus.ACTIVE))
                .thenReturn(List.of(electricityRu));
        when(typeRepository.findByIdAndOwnerId(2L, 1L)).thenReturn(Optional.of(electricity));

        UtilityMeter meter = UtilityMeter.builder().id(92L).owner(owner).houseId(10L).roomId(20L)
                .utilityTypeId(2L).status(UtilityMeterStatus.ACTIVE).build();
        when(meterRepository.findByRoomIdAndUtilityTypeId(20L, 2L)).thenReturn(List.of(meter));
        UtilityReading baseline = UtilityReading.builder().id(7L).meter(meter)
                .readingDate(LocalDate.of(2026, 1, 31))
                .currentReading(new BigDecimal("21")).build();
        UtilityReading current = UtilityReading.builder().id(8L).meter(meter)
                .readingDate(LocalDate.of(2026, 2, 28))
                .currentReading(new BigDecimal("33")).build();
        when(invoiceRepository.findReadingsForPeriod(meter.getId(), LocalDate.of(2026, 1, 31)))
                .thenReturn(List.of(baseline));
        when(invoiceRepository.findReadingsForPeriod(meter.getId(), LocalDate.of(2026, 2, 28)))
                .thenReturn(List.of(current));
        when(invoiceRepository.findEffectiveRate(1L, 10L, 2L, LocalDate.of(2026, 2, 28),
                UtilityRateStatus.ACTIVE)).thenReturn(List.of(rate(2L, new BigDecimal("0.1300"))));

        InvoicePreviewResponse resp = invoiceService.previewInvoice(
                new GenerateInvoiceRequest(30L, 2026, 2, null, null));

        // usage 33 - 21 = 12 at 0.1300 -> 1.56 ; rent 150 -> 151.56
        assertThat(resp.items()).hasSize(2);
        var elecItem = resp.items().stream()
                .filter(i -> i.itemType() == InvoiceItemType.ELECTRICITY).findFirst().orElseThrow();
        assertThat(elecItem.quantity()).isEqualByComparingTo("12");
        assertThat(elecItem.unitPrice()).isEqualByComparingTo("0.1300");
        assertThat(elecItem.amount()).isEqualByComparingTo("1.56");
        assertThat(resp.total()).isEqualByComparingTo("151.56");
    }

    @Test
    void fixedUtilityAddsFlatCharge() {
        authAsOwner();
        stubBillingScene();
        UtilityType parking = fixedType(4L, "PARKING", "Parking");
        when(roomUtilityRepository.findByRoomIdAndStatus(20L, UtilityTypeStatus.ACTIVE))
                .thenReturn(List.of(roomUtility(4L)));
        when(typeRepository.findByIdAndOwnerId(4L, 1L)).thenReturn(Optional.of(parking));
        when(invoiceRepository.findEffectiveRate(1L, 10L, 4L, LocalDate.of(2026, 2, 28),
                UtilityRateStatus.ACTIVE)).thenReturn(List.of(rate(4L, new BigDecimal("20.0000"))));

        InvoicePreviewResponse resp = invoiceService.previewInvoice(
                new GenerateInvoiceRequest(30L, 2026, 2, null, null));

        assertThat(resp.items()).hasSize(2);
        var parkingItem = resp.items().stream()
                .filter(i -> i.itemType() == InvoiceItemType.PARKING).findFirst().orElseThrow();
        assertThat(parkingItem.quantity()).isEqualByComparingTo("1");
        assertThat(parkingItem.amount()).isEqualByComparingTo("20.00");
        assertThat(resp.total()).isEqualByComparingTo("170.00");
    }

    @Test
    void multipleMeteredUtilitiesOnSameRoomAreBilled() {
        authAsOwner();
        stubBillingScene();
        UtilityType water = meteredType(1L, "WATER", "Water");
        UtilityType electricity = meteredType(2L, "ELECTRICITY", "Electricity");
        // isIncluded only reflects that the utility is covered by the rent-fee
        // agreement for listing purposes; it must NOT suppress billing.
        RoomUtility waterRu = roomUtility(1L);
        waterRu.setIsIncluded(true);
        RoomUtility elecRu = roomUtility(2L);
        elecRu.setIsIncluded(true);
        when(roomUtilityRepository.findByRoomIdAndStatus(20L, UtilityTypeStatus.ACTIVE))
                .thenReturn(List.of(waterRu, elecRu));
        when(typeRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(water));
        when(typeRepository.findByIdAndOwnerId(2L, 1L)).thenReturn(Optional.of(electricity));

        UtilityMeter waterMeter = UtilityMeter.builder().id(90L).owner(owner).houseId(10L).roomId(20L)
                .utilityTypeId(1L).status(UtilityMeterStatus.ACTIVE).build();
        UtilityMeter elecMeter = UtilityMeter.builder().id(91L).owner(owner).houseId(10L).roomId(20L)
                .utilityTypeId(2L).status(UtilityMeterStatus.ACTIVE).build();
        when(meterRepository.findByRoomIdAndUtilityTypeId(20L, 1L)).thenReturn(List.of(waterMeter));
        when(meterRepository.findByRoomIdAndUtilityTypeId(20L, 2L)).thenReturn(List.of(elecMeter));

        when(invoiceRepository.findReadingsForPeriod(waterMeter.getId(), LocalDate.of(2026, 1, 31)))
                .thenReturn(List.of(UtilityReading.builder().id(1L).meter(waterMeter)
                        .readingDate(LocalDate.of(2026, 1, 31))
                        .currentReading(new BigDecimal("100")).build()));
        when(invoiceRepository.findReadingsForPeriod(waterMeter.getId(), LocalDate.of(2026, 2, 28)))
                .thenReturn(List.of(UtilityReading.builder().id(2L).meter(waterMeter)
                        .readingDate(LocalDate.of(2026, 2, 28))
                        .currentReading(new BigDecimal("150")).build()));
        when(invoiceRepository.findReadingsForPeriod(elecMeter.getId(), LocalDate.of(2026, 1, 31)))
                .thenReturn(List.of(UtilityReading.builder().id(3L).meter(elecMeter)
                        .readingDate(LocalDate.of(2026, 1, 31))
                        .currentReading(new BigDecimal("1000")).build()));
        when(invoiceRepository.findReadingsForPeriod(elecMeter.getId(), LocalDate.of(2026, 2, 28)))
                .thenReturn(List.of(UtilityReading.builder().id(4L).meter(elecMeter)
                        .readingDate(LocalDate.of(2026, 2, 28))
                        .currentReading(new BigDecimal("1100")).build()));

        when(invoiceRepository.findEffectiveRate(1L, 10L, 1L, LocalDate.of(2026, 2, 28),
                UtilityRateStatus.ACTIVE)).thenReturn(List.of(rate(1L, new BigDecimal("2.0000"))));
        when(invoiceRepository.findEffectiveRate(1L, 10L, 2L, LocalDate.of(2026, 2, 28),
                UtilityRateStatus.ACTIVE)).thenReturn(List.of(rate(2L, new BigDecimal("0.5000"))));

        InvoicePreviewResponse resp = invoiceService.previewInvoice(
                new GenerateInvoiceRequest(30L, 2026, 2, null, null));

        // rent 150 + water (50 * 2) + electricity (100 * 0.5) = 300
        assertThat(resp.items()).hasSize(3);
        var waterItem = resp.items().stream()
                .filter(i -> i.itemType() == InvoiceItemType.WATER).findFirst().orElseThrow();
        assertThat(waterItem.amount()).isEqualByComparingTo("100.00");
        var elecItem = resp.items().stream()
                .filter(i -> i.itemType() == InvoiceItemType.ELECTRICITY).findFirst().orElseThrow();
        assertThat(elecItem.quantity()).isEqualByComparingTo("100");
        assertThat(elecItem.amount()).isEqualByComparingTo("50.00");
        assertThat(resp.total()).isEqualByComparingTo("300.00");
    }

    @Test
    void fixedUtilityWithoutRateFails() {
        authAsOwner();
        stubBillingScene();
        UtilityType parking = fixedType(4L, "PARKING", "Parking");
        when(roomUtilityRepository.findByRoomIdAndStatus(20L, UtilityTypeStatus.ACTIVE))
                .thenReturn(List.of(roomUtility(4L)));
        when(typeRepository.findByIdAndOwnerId(4L, 1L)).thenReturn(Optional.of(parking));
        when(invoiceRepository.findEffectiveRate(1L, 10L, 4L, LocalDate.of(2026, 2, 28),
                UtilityRateStatus.ACTIVE)).thenReturn(List.of());

        assertThatThrownBy(() -> invoiceService.previewInvoice(
                new GenerateInvoiceRequest(30L, 2026, 2, null, null)))
                .isInstanceOf(InvalidInvoiceOperationException.class)
                .hasMessageContaining("No applicable rate");
    }

    @Test
    void meteredUtilityWithoutRateFails() {
        authAsOwner();
        stubBillingScene();
        UtilityType electricity = meteredType(2L, "ELECTRICITY", "Electricity");
        when(roomUtilityRepository.findByRoomIdAndStatus(20L, UtilityTypeStatus.ACTIVE))
                .thenReturn(List.of(roomUtility(2L)));
        when(typeRepository.findByIdAndOwnerId(2L, 1L)).thenReturn(Optional.of(electricity));
        UtilityMeter meter = UtilityMeter.builder().id(91L).owner(owner).houseId(10L).roomId(20L)
                .utilityTypeId(2L).status(UtilityMeterStatus.ACTIVE).build();
        when(meterRepository.findByRoomIdAndUtilityTypeId(20L, 2L)).thenReturn(List.of(meter));
        when(invoiceRepository.findReadingsForPeriod(meter.getId(), LocalDate.of(2026, 1, 31)))
                .thenReturn(List.of(UtilityReading.builder().id(5L).meter(meter)
                        .readingDate(LocalDate.of(2026, 1, 31))
                        .currentReading(new BigDecimal("1000")).build()));
        when(invoiceRepository.findReadingsForPeriod(meter.getId(), LocalDate.of(2026, 2, 28)))
                .thenReturn(List.of(UtilityReading.builder().id(6L).meter(meter)
                        .readingDate(LocalDate.of(2026, 2, 28))
                        .currentReading(new BigDecimal("1100")).build()));
        when(invoiceRepository.findEffectiveRate(1L, 10L, 2L, LocalDate.of(2026, 2, 28),
                UtilityRateStatus.ACTIVE)).thenReturn(List.of());

        // Meter + reading exist but no rate: the price is never invented.
        assertThatThrownBy(() -> invoiceService.previewInvoice(
                new GenerateInvoiceRequest(30L, 2026, 2, null, null)))
                .isInstanceOf(InvalidInvoiceOperationException.class)
                .hasMessageContaining("No applicable rate");
    }

    @Test
    void includedFixedUtilityIsStillBilled() {
        authAsOwner();
        stubBillingScene();
        UtilityType parking = fixedType(4L, "PARKING", "Parking");
        RoomUtility included = roomUtility(4L);
        included.setIsIncluded(true);
        when(roomUtilityRepository.findByRoomIdAndStatus(20L, UtilityTypeStatus.ACTIVE))
                .thenReturn(List.of(included));
        when(typeRepository.findByIdAndOwnerId(4L, 1L)).thenReturn(Optional.of(parking));
        when(invoiceRepository.findEffectiveRate(1L, 10L, 4L, LocalDate.of(2026, 2, 28),
                UtilityRateStatus.ACTIVE)).thenReturn(List.of(rate(4L, new BigDecimal("20.0000"))));

        InvoicePreviewResponse response = invoiceService.previewInvoice(
                new GenerateInvoiceRequest(30L, 2026, 2, null, null));

        // isIncluded=true does not suppress billing: the configured fixed price is
        // charged just like any other assigned utility.
        assertThat(response.items()).hasSize(2);
        var parkingItem = response.items().stream()
                .filter(i -> i.itemType() == InvoiceItemType.PARKING).findFirst().orElseThrow();
        assertThat(parkingItem.quantity()).isEqualByComparingTo("1");
        assertThat(parkingItem.amount()).isEqualByComparingTo("20.00");
        assertThat(response.total()).isEqualByComparingTo("170.00");
    }

    @Test
    void meteredUtilityWithoutActiveMeterIsSkipped() {
        authAsOwner();
        stubBillingScene();
        UtilityType water = meteredType(1L, "WATER", "Water");
        when(roomUtilityRepository.findByRoomIdAndStatus(20L, UtilityTypeStatus.ACTIVE))
                .thenReturn(List.of(roomUtility(1L)));
        when(typeRepository.findByIdAndOwnerId(1L, 1L)).thenReturn(Optional.of(water));
        when(meterRepository.findByRoomIdAndUtilityTypeId(20L, 1L)).thenReturn(List.of());

        InvoicePreviewResponse resp = invoiceService.previewInvoice(
                new GenerateInvoiceRequest(30L, 2026, 2, null, null));

        // No ACTIVE meter means the reading cannot be attributed: skipped, not failed.
        assertThat(resp.items()).hasSize(1);
        assertThat(resp.items().get(0).itemType()).isEqualTo(InvoiceItemType.RENT);
        assertThat(resp.total()).isEqualByComparingTo("150.00");
    }

    @Test
    void generateRejectsDuplicatePeriod() {
        authAsOwner();
        stubBillingScene();
        when(roomUtilityRepository.findByRoomIdAndStatus(20L, UtilityTypeStatus.ACTIVE))
                .thenReturn(List.of());
        when(invoiceRepository.existsByContractIdAndBillingYearAndBillingMonth(30L, 2026, 2))
                .thenReturn(true);

        assertThatThrownBy(() -> invoiceService.generateMonthlyInvoice(
                new GenerateInvoiceRequest(30L, 2026, 2, null, null)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void rejectCrossOwnerNonManager() {
        authAsOwner();
        RentalContract otherOwnerContract = RentalContract.builder().id(31L).ownerId(99L)
                .contractNo("CT-31").roomId(20L).monthlyRent(new BigDecimal("150.00"))
                .dueDay(5).status(RentalContractStatus.ACTIVE).build();
        when(contractRepository.findById(31L)).thenReturn(Optional.of(otherOwnerContract));

        assertThatThrownBy(() -> invoiceService.previewInvoice(
                new GenerateInvoiceRequest(31L, 2026, 2, null, null)))
                .isInstanceOf(com.example.spring_boot_project_api.exception.RentalContractNotFoundException.class);
    }

    @Test
    void bulkGenerateCreatesForActiveContracts() {
        authAsOwner();
        RentalContract contract2 = RentalContract.builder().id(32L).ownerId(1L).contractNo("CT-32")
                .roomId(21L).startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2027, 1, 1))
                .monthlyRent(new BigDecimal("200.00"))
                .dueDay(5).status(RentalContractStatus.ACTIVE).build();

        when(contractRepository.findByRoomHouseIdInAndStatus(any(), eq(RentalContractStatus.ACTIVE)))
                .thenReturn(List.of(contract, contract2));
        when(contractRepository.findById(30L)).thenReturn(Optional.of(contract));
        when(contractRepository.findById(32L)).thenReturn(Optional.of(contract2));

        // Contract 1 has no utilities -> rent only
        when(roomUtilityRepository.findByRoomIdAndStatus(20L, UtilityTypeStatus.ACTIVE))
                .thenReturn(List.of());
        when(invoiceRepository.existsByContractIdAndBillingYearAndBillingMonth(30L, 2026, 2))
                .thenReturn(false);
        when(invoiceRepository.existsByContractIdAndBillingYearAndBillingMonth(32L, 2026, 2))
                .thenReturn(false);

        // Rooms & houses for both
        when(roomRepository.findById(20L)).thenReturn(Optional.of(room));
        when(houseRepository.findById(10L)).thenReturn(Optional.of(house));
        when(contractTenantRepository.findByContractId(30L)).thenReturn(List.of());
        when(contractTenantRepository.findByContractId(32L)).thenReturn(List.of());

        // Room 2 setup for contract2
        Room room2 = new Room();
        room2.setId(21L);
        room2.setOwnerId(1L);
        room2.setHouseId(10L);
        room2.setRoomNumber("A-2");
        room2.setMonthlyRent(new BigDecimal("200.00"));
        when(roomRepository.findById(21L)).thenReturn(Optional.of(room2));
        when(roomUtilityRepository.findByRoomIdAndStatus(21L, UtilityTypeStatus.ACTIVE))
                .thenReturn(List.of());

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice in = inv.getArgument(0);
            return Invoice.builder()
                    .id(in.getId() == null ? 100L : in.getId())
                    .owner(owner)
                    .invoiceNo(in.getInvoiceNo())
                    .contractId(in.getContractId())
                    .billingYear(in.getBillingYear())
                    .billingMonth(in.getBillingMonth())
                    .dueDate(in.getDueDate())
                    .subtotal(in.getSubtotal())
                    .discount(in.getDiscount())
                    .lateFee(in.getLateFee())
                    .totalAmount(in.getTotalAmount())
                    .status(in.getStatus())
                    .items(in.getItems())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        });

        when(invoiceMapper.toResponse(any(Invoice.class), any(Object[].class), any(BigDecimal.class)))
                .thenAnswer(inv -> {
                    Invoice in = inv.getArgument(0);
                    return new com.example.spring_boot_project_api.dto.response.InvoiceResponse(
                            in.getId() == null ? 100L : in.getId(), 1L, in.getInvoiceNo(),
                            in.getContractId(), "CT", null, null, null, null, null, null,
                            in.getBillingYear(), in.getBillingMonth(), in.getDueDate(),
                            in.getSubtotal(), in.getDiscount(), in.getLateFee(),
                            in.getTotalAmount(), BigDecimal.ZERO, in.getTotalAmount(),
                            in.getStatus(), null, null, null, null, List.of());
                });

        // Invoice number counting
        when(invoiceRepository.count()).thenReturn(5L);

        BulkGenerateInvoiceResponse resp = invoiceService.generateBulkInvoices(
                new BulkGenerateInvoiceRequest(2026, 2, null, null, null));

        assertThat(resp.createdCount()).isEqualTo(2);
        assertThat(resp.skippedCount()).isEqualTo(0);
        assertThat(resp.errorCount()).isEqualTo(0);
        assertThat(resp.createdInvoices()).hasSize(2);
    }

    @Test
    void bulkGenerateSkipsExisting() {
        authAsOwner();
        when(contractRepository.findByRoomHouseIdInAndStatus(any(), eq(RentalContractStatus.ACTIVE)))
                .thenReturn(List.of(contract));
        when(contractRepository.findById(30L)).thenReturn(Optional.of(contract));
        when(roomRepository.findById(20L)).thenReturn(Optional.of(room));
        when(roomUtilityRepository.findByRoomIdAndStatus(20L, UtilityTypeStatus.ACTIVE))
                .thenReturn(List.of());
        when(houseRepository.findById(10L)).thenReturn(Optional.of(house));
        when(contractTenantRepository.findByContractId(30L)).thenReturn(List.of());
        when(invoiceRepository.existsByContractIdAndBillingYearAndBillingMonth(30L, 2026, 2))
                .thenReturn(true);

        BulkGenerateInvoiceResponse resp = invoiceService.generateBulkInvoices(
                new BulkGenerateInvoiceRequest(2026, 2, null, null, null));

        assertThat(resp.createdCount()).isEqualTo(0);
        assertThat(resp.skippedCount()).isEqualTo(1);
        assertThat(resp.errorCount()).isEqualTo(0);
        assertThat(resp.skippedReasons()).hasSize(1);
    }

    @Test
    void bulkGenerateCollectsErrors() {
        authAsOwner();
        RentalContract badContract = RentalContract.builder().id(33L).ownerId(1L).contractNo("CT-33")
                .roomId(22L).startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2027, 1, 1))
                .monthlyRent(new BigDecimal("100.00"))
                .dueDay(5).status(RentalContractStatus.ACTIVE).build();
        when(contractRepository.findByRoomHouseIdInAndStatus(any(), eq(RentalContractStatus.ACTIVE)))
                .thenReturn(List.of(badContract));
        when(contractRepository.findById(33L)).thenReturn(Optional.of(badContract));

        // No room found -> triggers error since room is null
        when(roomRepository.findById(22L)).thenReturn(Optional.empty());
        when(invoiceRepository.existsByContractIdAndBillingYearAndBillingMonth(33L, 2026, 2))
                .thenReturn(false);

        BulkGenerateInvoiceResponse resp = invoiceService.generateBulkInvoices(
                new BulkGenerateInvoiceRequest(2026, 2, null, null, null));

        assertThat(resp.createdCount()).isEqualTo(0);
        assertThat(resp.errorCount()).isEqualTo(1);
        assertThat(resp.errors()).hasSize(1);
    }

    @Test
    void eligibleContractsListsActiveWithDetails() {
        authAsOwner();
        PageRequest pageable = PageRequest.of(0, 10);
        when(contractRepository.findEligibleForInvoice(
                any(), any(), eq(RentalContractStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(contract), pageable, 1));
        when(roomRepository.findAllById(any())).thenReturn(List.of(room));
        when(houseRepository.findAllById(any())).thenReturn(List.of(house));

        ContractTenant link = ContractTenant.builder().id(5L).contractId(30L).tenantId(40L)
                .isPrimary(true).joinedAt(LocalDate.of(2026, 1, 1)).build();
        Tenant tenant = Tenant.builder().id(40L).firstName("Sok").lastName("Dara").build();
        when(contractTenantRepository.findByContractIdIn(any())).thenReturn(List.of(link));
        when(tenantRepository.findAllById(any())).thenReturn(List.of(tenant));
        when(invoiceRepository.findInvoicedContractIds(any(), eq(2026), eq(2)))
                .thenReturn(List.of(30L));

        PageResponse<ContractEligibleResponse> resp =
                invoiceService.getEligibleContracts(null, "Dara", 2026, 2, pageable);

        assertThat(resp.totalElements()).isEqualTo(1);
        ContractEligibleResponse row = resp.content().get(0);
        assertThat(row.contractId()).isEqualTo(30L);
        assertThat(row.contractNo()).isEqualTo("CT-30");
        assertThat(row.roomNumber()).isEqualTo("A-1");
        assertThat(row.houseName()).isEqualTo("Mansion");
        assertThat(row.tenantName()).isEqualTo("Sok Dara");
        assertThat(row.alreadyInvoiced()).isTrue();
    }

    @Test
    void eligibleContractsEmptyWhenNoAccessibleHouses() {
        authAsOwner();
        when(houseAccessService.getAccessibleHouseIds()).thenReturn(List.of());

        PageResponse<ContractEligibleResponse> resp =
                invoiceService.getEligibleContracts(null, null, null, null, PageRequest.of(0, 10));

        assertThat(resp.content()).isEmpty();
        assertThat(resp.totalElements()).isZero();
        verify(contractRepository, never()).findEligibleForInvoice(any(), any(), any(), any());
    }

    @Test
    void eligibleContractsRequiresAccessAndScopesToHouse() {
        authAsOwner();
        PageRequest pageable = PageRequest.of(0, 10);
        when(contractRepository.findEligibleForInvoice(
                any(), any(), eq(RentalContractStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        invoiceService.getEligibleContracts(10L, null, null, null, pageable);

        verify(houseAccessService).requireHouseAccess(10L);
        verify(contractRepository).findEligibleForInvoice(
                eq(List.of(10L)), any(), eq(RentalContractStatus.ACTIVE), any(Pageable.class));
    }

    @Test
    void eligibleContractsRejectsUsersWithoutOwnerOrManagerRole() {
        authAsOwner();
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);
        when(securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)).thenReturn(false);

        assertThatThrownBy(() -> invoiceService.getEligibleContracts(
                null, null, null, null, PageRequest.of(0, 10)))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void previewBulkGenerationCountsActiveAndAlreadyInvoiced() {
        authAsOwner();
        when(contractRepository.countActiveByHouseIds(any(), eq(RentalContractStatus.ACTIVE)))
                .thenReturn(3L);
        when(contractRepository.countAlreadyInvoicedByHouseIds(
                any(), eq(RentalContractStatus.ACTIVE), eq(2026), eq(2))).thenReturn(1L);

        GenerateBulkPreviewResponse resp = invoiceService.previewBulkGeneration(null, 2026, 2);

        assertThat(resp.activeContracts()).isEqualTo(3L);
        assertThat(resp.alreadyInvoiced()).isEqualTo(1L);
        assertThat(resp.toGenerate()).isEqualTo(2L);
        assertThat(resp.year()).isEqualTo(2026);
        assertThat(resp.month()).isEqualTo(2);
    }

    @Test
    void previewBulkGenerationZeroWhenNoAccessibleHouses() {
        authAsOwner();
        when(houseAccessService.getAccessibleHouseIds()).thenReturn(List.of());

        GenerateBulkPreviewResponse resp = invoiceService.previewBulkGeneration(null, 2026, 2);

        assertThat(resp.activeContracts()).isZero();
        assertThat(resp.alreadyInvoiced()).isZero();
        assertThat(resp.toGenerate()).isZero();
        verify(contractRepository, never()).countActiveByHouseIds(any(), any());
    }

    @Test
    void previewBulkGenerationRejectsInvalidPeriod() {
        authAsOwner();

        assertThatThrownBy(() -> invoiceService.previewBulkGeneration(null, 2026, 13))
                .isInstanceOf(InvalidInvoiceOperationException.class)
                .hasMessageContaining("between 1 and 12");

        assertThatThrownBy(() -> invoiceService.previewBulkGeneration(null, 1999, 2))
                .isInstanceOf(InvalidInvoiceOperationException.class)
                .hasMessageContaining("at least 2000");
    }

    @Test
    void bulkGenerateWithHouseFilterUsesHouseScope() {
        authAsOwner();
        when(contractRepository.findByRoomHouseIdInAndStatus(any(), eq(RentalContractStatus.ACTIVE)))
                .thenReturn(List.of());

        BulkGenerateInvoiceResponse resp = invoiceService.generateBulkInvoices(
                new BulkGenerateInvoiceRequest(2026, 2, null, null, 10L));

        assertThat(resp.createdCount()).isZero();
        verify(houseAccessService).requireHouseAccess(10L);
        verify(contractRepository).findByRoomHouseIdInAndStatus(
                eq(List.of(10L)), eq(RentalContractStatus.ACTIVE));
    }
}
