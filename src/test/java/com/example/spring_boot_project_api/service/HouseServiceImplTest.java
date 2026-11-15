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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.spring_boot_project_api.dto.response.HouseSummaryResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.RentalContractResponse;
import com.example.spring_boot_project_api.dto.response.RoomResponse;
import com.example.spring_boot_project_api.dto.response.TenantResponse;
import com.example.spring_boot_project_api.enums.RentalContractStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.exception.HouseNotFoundException;
import com.example.spring_boot_project_api.mapper.HouseMapper;
import com.example.spring_boot_project_api.mapper.RentalContractMapper;
import com.example.spring_boot_project_api.mapper.RoomMapper;
import com.example.spring_boot_project_api.mapper.TenantMapper;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.HouseStatus;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.model.TenantStatus;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserStatus;
import com.example.spring_boot_project_api.repository.ContractTenantRepository;
import com.example.spring_boot_project_api.repository.HouseImageRepository;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.service.impl.HouseServiceImpl;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
class HouseServiceImplTest {

    @Mock
    private HouseRepository houseRepository;
    @Mock
    private HouseImageRepository houseImageRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private RentalContractRepository rentalContractRepository;
    @Mock
    private ContractTenantRepository contractTenantRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private UserHouseAccessRepository userHouseAccessRepository;
    @Mock
    private SecurityUtils securityUtils;

    @Spy
    private HouseMapper houseMapper = new HouseMapper();
    @Spy
    private RoomMapper roomMapper = new RoomMapper();
    @Spy
    private TenantMapper tenantMapper = new TenantMapper();
    @Spy
    private RentalContractMapper rentalContractMapper = new RentalContractMapper();

    @InjectMocks
    private HouseServiceImpl houseService;

    private User owner;
    private House house;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(15L)
                .username("managerA")
                .email("managerA@example.com")
                .passwordHash("hashed")
                .firstName("Manager")
                .lastName("A")
                .phone("+85512345")
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build();

        house = House.builder()
                .id(3L)
                .owner(owner)
                .name("Sunrise Residence")
                .slug("sunrise-residence")
                .code("SR001")
                .address("#123, Street 271")
                .postalCode("12352")
                .totalRooms(200)
                .availableRooms(150)
                .status(HouseStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2026, 8, 23, 22, 55, 25))
                .updatedAt(LocalDateTime.of(2026, 8, 26, 15, 11, 46))
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(Long userId) {
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
    }

    private Room room(Long id, RoomStatus status) {
        Room r = new Room();
        r.setId(id);
        r.setOwnerId(15L);
        r.setHouseId(3L);
        r.setRoomTypeId(1L);
        r.setRoomNumber("R" + id);
        r.setName("Room " + id);
        r.setMonthlyRent(BigDecimal.valueOf(150));
        r.setSecurityDeposit(BigDecimal.valueOf(300));
        r.setCapacity(1);
        r.setStatus(status);
        r.setCreatedAt(LocalDateTime.of(2026, 8, 23, 22, 55, 25));
        r.setUpdatedAt(LocalDateTime.of(2026, 8, 23, 22, 55, 25));
        return r;
    }

    private RentalContract contract(Long id, Long roomId, RentalContractStatus status) {
        RentalContract rc = new RentalContract();
        rc.setId(id);
        rc.setOwnerId(15L);
        rc.setContractNo("C" + id);
        rc.setRoomId(roomId);
        rc.setStartDate(LocalDate.of(2026, 1, 1));
        rc.setEndDate(LocalDate.of(2027, 1, 1));
        rc.setMonthlyRent(BigDecimal.valueOf(150));
        rc.setSecurityDeposit(BigDecimal.valueOf(300));
        rc.setDueDay(5);
        rc.setStatus(status);
        rc.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        rc.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        return rc;
    }

    private Tenant tenant(Long id) {
        return Tenant.builder()
                .id(id)
                .user(owner)
                .owner(owner)
                .tenantCode("T" + id)
                .firstName("Tenant")
                .lastName(String.valueOf(id))
                .phone("+855111")
                .status(TenantStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build();
    }

    // ------------------------------------------------------------------
    // Security / owner isolation
    // ------------------------------------------------------------------

    @Test
    void getHouseByIdReturnsHouseForOwner() {
        authenticateAs(15L);
        when(houseRepository.findById(3L)).thenReturn(Optional.of(house));

        var response = houseService.getHouseById(3L);

        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.ownerId()).isEqualTo(15L);
        assertThat(response.name()).isEqualTo("Sunrise Residence");
    }

    @Test
    void getHouseByIdThrowsNotFoundWhenMissing() {
        authenticateAs(15L);
        when(houseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> houseService.getHouseById(99L))
                .isInstanceOf(HouseNotFoundException.class);
    }

    @Test
    void getHouseByIdThrowsForbiddenForAnotherOwner() {
        authenticateAs(15L);
        house.getOwner().setId(20L);
        when(houseRepository.findById(3L)).thenReturn(Optional.of(house));

        assertThatThrownBy(() -> houseService.getHouseById(3L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getHouseSummaryThrowsForbiddenForAnotherOwner() {
        authenticateAs(15L);
        house.getOwner().setId(20L);
        when(houseRepository.findById(3L)).thenReturn(Optional.of(house));

        assertThatThrownBy(() -> houseService.getHouseSummary(3L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getHouseRoomsThrowsForbiddenForAnotherOwner() {
        authenticateAs(15L);
        house.getOwner().setId(20L);
        when(houseRepository.findById(3L)).thenReturn(Optional.of(house));

        assertThatThrownBy(() -> houseService.getHouseRooms(3L, null, null, null,
                PageRequest.of(0, 10)))
                .isInstanceOf(AccessDeniedException.class);
        verify(roomRepository, never()).findByHouseIdWithFilters(any(), any(), any(), any(), any(), any());
    }

    @Test
    void getHouseTenantsThrowsForbiddenForAnotherOwner() {
        authenticateAs(15L);
        house.getOwner().setId(20L);
        when(houseRepository.findById(3L)).thenReturn(Optional.of(house));

        assertThatThrownBy(() -> houseService.getHouseTenants(3L, null, PageRequest.of(0, 10)))
                .isInstanceOf(AccessDeniedException.class);
        verify(tenantRepository, never()).findActiveTenantsByHouseId(any(), any(), any(), any(), any(), any());
    }

    @Test
    void getHouseContractsThrowsForbiddenForAnotherOwner() {
        authenticateAs(15L);
        house.getOwner().setId(20L);
        when(houseRepository.findById(3L)).thenReturn(Optional.of(house));

        assertThatThrownBy(() -> houseService.getHouseContracts(3L, null, null, PageRequest.of(0, 10)))
                .isInstanceOf(AccessDeniedException.class);
        verify(rentalContractRepository, never()).findByHouseIdWithFilters(any(), any(), any(), any(), any());
    }

    @Test
    void getHouseByIdAllowsAssignedManager() {
        house.getOwner().setId(20L);
        authenticateAs(15L);
        when(userHouseAccessRepository.existsByUserIdAndHouseId(15L, 3L)).thenReturn(true);
        when(houseRepository.findById(3L)).thenReturn(Optional.of(house));

        var response = houseService.getHouseById(3L);

        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.name()).isEqualTo("Sunrise Residence");
    }

    @Test
    void getHouseByIdDeniesUnassignedManager() {
        house.getOwner().setId(20L);
        authenticateAs(15L);
        when(userHouseAccessRepository.existsByUserIdAndHouseId(15L, 3L)).thenReturn(false);
        when(houseRepository.findById(3L)).thenReturn(Optional.of(house));

        assertThatThrownBy(() -> houseService.getHouseById(3L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getHouseSummaryUsesManagerCountsForAssignedManager() {
        house.getOwner().setId(20L);
        authenticateAs(15L);
        when(userHouseAccessRepository.existsByUserIdAndHouseId(15L, 3L)).thenReturn(true);
        when(houseRepository.findById(3L)).thenReturn(Optional.of(house));
        when(roomRepository.countByHouseId(3L)).thenReturn(10L);
        when(roomRepository.countByHouseIdAndStatus(3L, RoomStatus.AVAILABLE)).thenReturn(4L);
        when(roomRepository.countByHouseIdAndStatus(3L, RoomStatus.OCCUPIED)).thenReturn(6L);
        when(roomRepository.countByHouseIdAndStatus(3L, RoomStatus.MAINTENANCE)).thenReturn(0L);
        when(contractTenantRepository.countActiveTenantsByHouseId(eq(3L), any(LocalDate.class)))
                .thenReturn(6L);
        when(rentalContractRepository.countByHouseIdAndStatus(3L, RentalContractStatus.ACTIVE))
                .thenReturn(3L);

        HouseSummaryResponse summary = houseService.getHouseSummary(3L);

        assertThat(summary.totalRooms()).isEqualTo(10L);
        assertThat(summary.occupiedRooms()).isEqualTo(6L);
        assertThat(summary.activeTenants()).isEqualTo(6L);
        assertThat(summary.activeContracts()).isEqualTo(3L);
        verify(contractTenantRepository, never()).countActiveTenantsByHouseIdAndOwnerId(any(), any(), any());
    }

    // ------------------------------------------------------------------
    // Summary
    // ------------------------------------------------------------------

    @Test
    void getHouseSummaryComputesAllCountsFromDatabase() {
        authenticateAs(15L);
        when(houseRepository.findById(3L)).thenReturn(Optional.of(house));
        when(roomRepository.countByHouseId(3L)).thenReturn(200L);
        when(roomRepository.countByHouseIdAndStatus(3L, RoomStatus.AVAILABLE)).thenReturn(150L);
        when(roomRepository.countByHouseIdAndStatus(3L, RoomStatus.OCCUPIED)).thenReturn(45L);
        when(roomRepository.countByHouseIdAndStatus(3L, RoomStatus.MAINTENANCE)).thenReturn(5L);
        when(contractTenantRepository.countActiveTenantsByHouseIdAndOwnerId(eq(3L), eq(15L), any(LocalDate.class)))
                .thenReturn(52L);
        when(rentalContractRepository.countByHouseIdAndOwnerIdAndStatus(
                3L, 15L, RentalContractStatus.ACTIVE)).thenReturn(45L);

        HouseSummaryResponse summary = houseService.getHouseSummary(3L);

        assertThat(summary.totalRooms()).isEqualTo(200L);
        assertThat(summary.availableRooms()).isEqualTo(150L);
        assertThat(summary.occupiedRooms()).isEqualTo(45L);
        assertThat(summary.maintenanceRooms()).isEqualTo(5L);
        assertThat(summary.activeTenants()).isEqualTo(52L);
        assertThat(summary.activeContracts()).isEqualTo(45L);
    }

    @Test
    void getHouseSummaryHandlesHouseWithNoRooms() {
        authenticateAs(15L);
        when(houseRepository.findById(3L)).thenReturn(Optional.of(house));
        when(roomRepository.countByHouseId(3L)).thenReturn(0L);
        when(roomRepository.countByHouseIdAndStatus(3L, RoomStatus.AVAILABLE)).thenReturn(0L);
        when(roomRepository.countByHouseIdAndStatus(3L, RoomStatus.OCCUPIED)).thenReturn(0L);
        when(roomRepository.countByHouseIdAndStatus(3L, RoomStatus.MAINTENANCE)).thenReturn(0L);
        when(contractTenantRepository.countActiveTenantsByHouseIdAndOwnerId(eq(3L), eq(15L), any(LocalDate.class)))
                .thenReturn(0L);
        when(rentalContractRepository.countByHouseIdAndOwnerIdAndStatus(
                3L, 15L, RentalContractStatus.ACTIVE)).thenReturn(0L);

        HouseSummaryResponse summary = houseService.getHouseSummary(3L);

        assertThat(summary.totalRooms()).isZero();
        assertThat(summary.availableRooms()).isZero();
        assertThat(summary.occupiedRooms()).isZero();
        assertThat(summary.maintenanceRooms()).isZero();
        assertThat(summary.activeTenants()).isZero();
        assertThat(summary.activeContracts()).isZero();
    }

    @Test
    void getHouseSummaryIgnoresRoomsBelongingToOtherHouses() {
        authenticateAs(15L);
        when(houseRepository.findById(3L)).thenReturn(Optional.of(house));
        // Room counts are scoped to houseId 3
        when(roomRepository.countByHouseId(3L)).thenReturn(10L);
        when(roomRepository.countByHouseIdAndStatus(3L, RoomStatus.AVAILABLE)).thenReturn(4L);
        when(roomRepository.countByHouseIdAndStatus(3L, RoomStatus.OCCUPIED)).thenReturn(6L);
        when(roomRepository.countByHouseIdAndStatus(3L, RoomStatus.MAINTENANCE)).thenReturn(0L);
        when(contractTenantRepository.countActiveTenantsByHouseIdAndOwnerId(eq(3L), eq(15L), any(LocalDate.class)))
                .thenReturn(6L);
        when(rentalContractRepository.countByHouseIdAndOwnerIdAndStatus(
                3L, 15L, RentalContractStatus.ACTIVE)).thenReturn(3L);

        HouseSummaryResponse summary = houseService.getHouseSummary(3L);

        assertThat(summary.totalRooms()).isEqualTo(10L);
        assertThat(summary.occupiedRooms()).isEqualTo(6L);
        assertThat(summary.activeContracts()).isEqualTo(3L);
    }

    // ------------------------------------------------------------------
    // Rooms pagination / filters
    // ------------------------------------------------------------------

    @Test
    void getHouseRoomsReturnsPaginatedRooms() {
        authenticateAs(15L);
        when(houseRepository.findById(3L)).thenReturn(Optional.of(house));
        Room r1 = room(1L, RoomStatus.AVAILABLE);
        Room r2 = room(2L, RoomStatus.OCCUPIED);
        Page<Room> page = new PageImpl<>(List.of(r1, r2), PageRequest.of(0, 10), 2L);
        when(roomRepository.findByHouseIdWithFilters(eq(3L), eq(15L), isNull(), isNull(), isNull(),
                any(Pageable.class))).thenReturn(page);

        PageResponse<RoomResponse> result = houseService.getHouseRooms(3L, null, null, null, PageRequest.of(0, 10));

        assertThat(result.content()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.page()).isZero();
        assertThat(result.content().get(0).getHouseId()).isEqualTo(3L);
    }

    @Test
    void getHouseRoomsAppliesSearchAndFilters() {
        authenticateAs(15L);
        when(houseRepository.findById(3L)).thenReturn(Optional.of(house));
        Room r1 = room(1L, RoomStatus.OCCUPIED);
        Page<Room> page = new PageImpl<>(List.of(r1), PageRequest.of(0, 5), 1L);
        when(roomRepository.findByHouseIdWithFilters(eq(3L), eq(15L), eq("Sun"), eq(RoomStatus.OCCUPIED),
                eq(2L), any(Pageable.class))).thenReturn(page);

        PageResponse<RoomResponse> result = houseService.getHouseRooms(3L, "Sun", RoomStatus.OCCUPIED, 2L,
                PageRequest.of(0, 5));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).getStatus()).isEqualTo(RoomStatus.OCCUPIED);
    }

    // ------------------------------------------------------------------
    // Tenants pagination / search
    // ------------------------------------------------------------------

    @Test
    void getHouseTenantsReturnsPaginatedDistinctTenants() {
        authenticateAs(15L);
        when(houseRepository.findById(3L)).thenReturn(Optional.of(house));
        Tenant t1 = tenant(1L);
        Page<Tenant> page = new PageImpl<>(List.of(t1), PageRequest.of(0, 10), 1L);
        when(tenantRepository.findActiveTenantsByHouseId(eq(3L), eq(15L), eq(RentalContractStatus.ACTIVE),
                any(LocalDate.class), isNull(), any(Pageable.class))).thenReturn(page);

        PageResponse<TenantResponse> result = houseService.getHouseTenants(3L, null, PageRequest.of(0, 10));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).id()).isEqualTo(1L);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void getHouseTenantsAppliesSearch() {
        authenticateAs(15L);
        when(houseRepository.findById(3L)).thenReturn(Optional.of(house));
        Tenant t1 = tenant(1L);
        Page<Tenant> page = new PageImpl<>(List.of(t1), PageRequest.of(0, 10), 1L);
        when(tenantRepository.findActiveTenantsByHouseId(eq(3L), eq(15L), eq(RentalContractStatus.ACTIVE),
                any(LocalDate.class), eq("Ten"), any(Pageable.class))).thenReturn(page);

        PageResponse<TenantResponse> result = houseService.getHouseTenants(3L, "Ten", PageRequest.of(0, 10));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).firstName()).isEqualTo("Tenant");
    }

    // ------------------------------------------------------------------
    // Contracts pagination / filters
    // ------------------------------------------------------------------

    @Test
    void getHouseContractsReturnsPaginatedContracts() {
        authenticateAs(15L);
        when(houseRepository.findById(3L)).thenReturn(Optional.of(house));
        RentalContract c1 = contract(1L, 1L, RentalContractStatus.ACTIVE);
        RentalContract c2 = contract(2L, 2L, RentalContractStatus.EXPIRED);
        Page<RentalContract> page = new PageImpl<>(List.of(c1, c2),
                PageRequest.of(0, 10, Sort.by("createdAt").descending()), 2L);
        when(rentalContractRepository.findByHouseIdWithFilters(eq(3L), eq(15L), isNull(), isNull(),
                any(Pageable.class))).thenReturn(page);

        PageResponse<RentalContractResponse> result = houseService.getHouseContracts(3L, null, null,
                PageRequest.of(0, 10));

        assertThat(result.content()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.content().get(0).getRoomId()).isEqualTo(1L);
    }

    @Test
    void getHouseContractsAppliesSearchAndStatus() {
        authenticateAs(15L);
        when(houseRepository.findById(3L)).thenReturn(Optional.of(house));
        RentalContract c1 = contract(1L, 1L, RentalContractStatus.ACTIVE);
        Page<RentalContract> page = new PageImpl<>(List.of(c1), PageRequest.of(0, 10), 1L);
        when(rentalContractRepository.findByHouseIdWithFilters(eq(3L), eq(15L), eq("C1"),
                eq(RentalContractStatus.ACTIVE), any(Pageable.class))).thenReturn(page);

        PageResponse<RentalContractResponse> result = houseService.getHouseContracts(3L, "C1",
                RentalContractStatus.ACTIVE, PageRequest.of(0, 10));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).getContractNo()).isEqualTo("C1");
        assertThat(result.content().get(0).getStatus()).isEqualTo(RentalContractStatus.ACTIVE);
    }
}
