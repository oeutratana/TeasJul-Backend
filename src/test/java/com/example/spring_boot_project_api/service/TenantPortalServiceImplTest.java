package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.spring_boot_project_api.dto.request.UpdateTenantRequest;
import com.example.spring_boot_project_api.dto.response.TenantMeResponse;
import com.example.spring_boot_project_api.dto.response.TenantUtilityResponse;
import com.example.spring_boot_project_api.enums.ContractDepositStatus;
import com.example.spring_boot_project_api.enums.RentalContractStatus;
import com.example.spring_boot_project_api.enums.UtilityMeterStatus;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.TenantNotFoundException;
import com.example.spring_boot_project_api.mapper.TenantPortalMapper;
import com.example.spring_boot_project_api.model.ContractDeposit;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.model.TenantStatus;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserStatus;
import com.example.spring_boot_project_api.model.UtilityMeter;
import com.example.spring_boot_project_api.model.UtilityReading;
import com.example.spring_boot_project_api.model.UtilityType;
import com.example.spring_boot_project_api.repository.ContractDepositRepository;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.repository.UtilityMeterRepository;
import com.example.spring_boot_project_api.repository.UtilityReadingRepository;
import com.example.spring_boot_project_api.repository.UtilityTypeRepository;
import com.example.spring_boot_project_api.service.impl.TenantContextResolver;
import com.example.spring_boot_project_api.service.impl.TenantPortalServiceImpl;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
class TenantPortalServiceImplTest {

    @Mock
    private TenantContextResolver tenantContextResolver;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private HouseRepository houseRepository;
    @Mock
    private ContractDepositRepository contractDepositRepository;
    @Mock
    private UtilityMeterRepository utilityMeterRepository;
    @Mock
    private UtilityReadingRepository utilityReadingRepository;
    @Mock
    private UtilityTypeRepository utilityTypeRepository;
    @Mock
    private SecurityUtils securityUtils;
    @Spy
    private TenantPortalMapper tenantPortalMapper = new TenantPortalMapper();

    @InjectMocks
    private TenantPortalServiceImpl tenantPortalService;

    private User currentUser() {
        return User.builder().id(10L).username("sopha").passwordHash("h")
                .firstName("Sopha").lastName("Chan").phone("012000111").email("sopha@example.com")
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    private User owner() {
        return User.builder().id(1L).username("ownerA").passwordHash("h")
                .firstName("Owner").lastName("A").phone("099888777").email("owner@example.com")
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    private Tenant tenantFor(User user, User owner, TenantStatus status) {
        return Tenant.builder().id(50L).user(user).owner(owner).tenantCode("TN-A01")
                .firstName("Sopha").lastName("Chan").phone("012000111").email("sopha@example.com")
                .status(status)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    private RentalContract activeContract() {
        return RentalContract.builder().id(900L).contractNo("C-001").roomId(12L)
                .startDate(LocalDate.now().minusMonths(2)).endDate(LocalDate.now().plusMonths(10))
                .monthlyRent(new BigDecimal("150000")).securityDeposit(new BigDecimal("300000"))
                .dueDay(5).status(RentalContractStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    private Room room() {
        Room room = new Room();
        room.setId(12L);
        room.setOwnerId(1L);
        room.setHouseId(8L);
        room.setRoomNumber("A101");
        room.setName("Room 101");
        room.setFloorNumber(1);
        room.setMonthlyRent(new BigDecimal("150000"));
        return room;
    }

    private House house() {
        return House.builder().id(8L).owner(owner()).name("Golden House").code("GH-01")
                .address("Phnom Penh").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getMyTenantProfileIncludesHouseRoomContractAndDeposit() {
        when(securityUtils.getCurrentUser()).thenReturn(currentUser());
        Tenant tenant = tenantFor(currentUser(), owner(), TenantStatus.RENTING);
        when(tenantContextResolver.requireLinkedTenant(10L)).thenReturn(tenant);
        when(tenantContextResolver.activeContractOf(50L)).thenReturn(activeContract());
        when(roomRepository.findById(12L)).thenReturn(Optional.of(room()));
        when(houseRepository.findById(8L)).thenReturn(Optional.of(house()));
        when(contractDepositRepository.findByRentalContractId(900L)).thenReturn(List.of(
                ContractDeposit.builder().rentalContract(activeContract())
                        .amount(new BigDecimal("300000"))
                        .paidAmount(new BigDecimal("300000")).status(ContractDepositStatus.PAID).build()));

        TenantMeResponse response = tenantPortalService.getMyTenantProfile();

        assertThat(response.id()).isEqualTo(50L);
        assertThat(response.status()).isEqualTo(TenantStatus.RENTING);
        assertThat(response.ownerName()).isEqualTo("Owner A");
        assertThat(response.ownerPhone()).isEqualTo("099888777");
        assertThat(response.house()).isNotNull();
        assertThat(response.house().name()).isEqualTo("Golden House");
        assertThat(response.room()).isNotNull();
        assertThat(response.room().roomNumber()).isEqualTo("A101");
        assertThat(response.contract()).isNotNull();
        assertThat(response.contract().contractNo()).isEqualTo("C-001");
        assertThat(response.contract().monthlyRent()).isEqualByComparingTo("150000");
        assertThat(response.contract().depositFullyPaid()).isTrue();
        assertThat(response.contract().depositPaidAmount()).isEqualByComparingTo("300000");
    }

    @Test
    void getMyTenantProfileReturnsNullContractWhilePending() {
        when(securityUtils.getCurrentUser()).thenReturn(currentUser());
        Tenant tenant = tenantFor(currentUser(), owner(), TenantStatus.PENDING);
        when(tenantContextResolver.requireLinkedTenant(10L)).thenReturn(tenant);
        when(tenantContextResolver.activeContractOf(50L)).thenReturn(null);

        TenantMeResponse response = tenantPortalService.getMyTenantProfile();

        assertThat(response.status()).isEqualTo(TenantStatus.PENDING);
        assertThat(response.contract()).isNull();
        assertThat(response.house()).isNull();
        assertThat(response.room()).isNull();
        verify(roomRepository, never()).findById(any());
        verify(contractDepositRepository, never()).findByRentalContractId(any());
    }

    @Test
    void getMyTenantProfileThrowsWhenNoLinkedTenant() {
        when(securityUtils.getCurrentUser()).thenReturn(currentUser());
        when(tenantContextResolver.requireLinkedTenant(10L))
                .thenThrow(new TenantNotFoundException("No tenant profile is linked to your account."));

        assertThatThrownBy(() -> tenantPortalService.getMyTenantProfile())
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void updateMyTenantProfileAppliesEditableFields() {
        when(securityUtils.getCurrentUser()).thenReturn(currentUser());
        Tenant tenant = tenantFor(currentUser(), owner(), TenantStatus.RENTING);
        when(tenantContextResolver.requireLinkedTenant(10L)).thenReturn(tenant);
        when(tenantContextResolver.activeContractOf(50L)).thenReturn(null);
        when(tenantRepository.existsByOwnerIdAndPhoneAndIdNot(1L, "012111222", 50L)).thenReturn(false);

        UpdateTenantRequest request = new UpdateTenantRequest(
                "Sopha", "Chan", null, null, "012111222", null, null, "Engineer",
                null, null, null, "Phnom Penh");

        TenantMeResponse response = tenantPortalService.updateMyTenantProfile(request);

        assertThat(tenant.getPhone()).isEqualTo("012111222");
        assertThat(tenant.getOccupation()).isEqualTo("Engineer");
        assertThat(tenant.getAddress()).isEqualTo("Phnom Penh");
        assertThat(response.id()).isEqualTo(50L);
        verify(tenantRepository).save(tenant);
    }

    @Test
    void updateMyTenantProfileRejectsDuplicatePhone() {
        when(securityUtils.getCurrentUser()).thenReturn(currentUser());
        Tenant tenant = tenantFor(currentUser(), owner(), TenantStatus.RENTING);
        when(tenantContextResolver.requireLinkedTenant(10L)).thenReturn(tenant);
        when(tenantRepository.existsByOwnerIdAndPhoneAndIdNot(1L, "012111222", 50L)).thenReturn(true);

        UpdateTenantRequest request = new UpdateTenantRequest(
                "Sopha", "Chan", null, null, "012111222", null, null, null, null, null, null, null);

        assertThatThrownBy(() -> tenantPortalService.updateMyTenantProfile(request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(tenantRepository, never()).save(any());
    }

    @Test
    void getMyUtilitiesReturnsMetersWithLatestReading() {
        when(securityUtils.getCurrentUser()).thenReturn(currentUser());
        Tenant tenant = tenantFor(currentUser(), owner(), TenantStatus.RENTING);
        when(tenantContextResolver.requireLinkedTenant(10L)).thenReturn(tenant);
        when(tenantContextResolver.activeContractOf(50L)).thenReturn(activeContract());
        when(roomRepository.findById(12L)).thenReturn(Optional.of(room()));

        UtilityType electric = UtilityType.builder().id(1L).owner(owner()).name("Electricity")
                .code("ELEC").unit("kWh").build();
        UtilityMeter meter = UtilityMeter.builder().id(7L).owner(owner()).houseId(8L).roomId(12L)
                .utilityTypeId(1L).meterNumber("M-100").status(UtilityMeterStatus.ACTIVE).build();
        UtilityReading reading = UtilityReading.builder().id(3L).meter(meter)
                .currentReading(new BigDecimal("1250.5")).usage(new BigDecimal("75.5"))
                .readingDate(LocalDate.now().minusDays(2)).build();

        when(utilityMeterRepository.findByRoomId(12L)).thenReturn(List.of(meter));
        when(utilityReadingRepository.findTopByMeterIdOrderByReadingDateDesc(7L))
                .thenReturn(Optional.of(reading));
        when(utilityTypeRepository.findById(1L)).thenReturn(Optional.of(electric));

        List<TenantUtilityResponse> utilities = tenantPortalService.getMyUtilities();

        assertThat(utilities).hasSize(1);
        TenantUtilityResponse item = utilities.get(0);
        assertThat(item.meterNumber()).isEqualTo("M-100");
        assertThat(item.utilityTypeName()).isEqualTo("Electricity");
        assertThat(item.unit()).isEqualTo("kWh");
        assertThat(item.lastReading()).isEqualByComparingTo("1250.5");
        assertThat(item.lastUsage()).isEqualByComparingTo("75.5");
    }

    @Test
    void getMyUtilitiesIsEmptyWhenNoActiveContract() {
        when(securityUtils.getCurrentUser()).thenReturn(currentUser());
        Tenant tenant = tenantFor(currentUser(), owner(), TenantStatus.PENDING);
        when(tenantContextResolver.requireLinkedTenant(10L)).thenReturn(tenant);
        when(tenantContextResolver.activeContractOf(50L)).thenReturn(null);

        List<TenantUtilityResponse> utilities = tenantPortalService.getMyUtilities();

        assertThat(utilities).isEmpty();
        verify(utilityMeterRepository, never()).findByRoomId(any());
    }
}