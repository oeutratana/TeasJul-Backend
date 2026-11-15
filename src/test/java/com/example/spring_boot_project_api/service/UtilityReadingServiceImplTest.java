package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.spring_boot_project_api.dto.request.MonthlyReadingBulkRequest;
import com.example.spring_boot_project_api.dto.response.ActiveRoomTenantDto;
import com.example.spring_boot_project_api.dto.response.BulkUtilityReadingResponse;
import com.example.spring_boot_project_api.dto.response.MonthlyReadingRowResponse;
import com.example.spring_boot_project_api.enums.ReadingStatusFilter;
import com.example.spring_boot_project_api.enums.UtilityMeterStatus;
import com.example.spring_boot_project_api.exception.HouseNotFoundException;
import com.example.spring_boot_project_api.exception.InvalidUtilityOperationException;
import com.example.spring_boot_project_api.mapper.UtilityReadingMapper;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserStatus;
import com.example.spring_boot_project_api.model.UtilityMeter;
import com.example.spring_boot_project_api.model.UtilityReading;
import com.example.spring_boot_project_api.model.UtilityType;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.UtilityMeterRepository;
import com.example.spring_boot_project_api.repository.UtilityReadingRepository;
import com.example.spring_boot_project_api.repository.UtilityTypeRepository;
import com.example.spring_boot_project_api.service.impl.UtilityReadingServiceImpl;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
class UtilityReadingServiceImplTest {

    private static final LocalDate SEPT_2026 = LocalDate.of(2026, 9, 30);

    @Mock
    private UtilityReadingRepository readingRepository;
    @Mock
    private UtilityMeterRepository meterRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserHouseAccessRepository userHouseAccessRepository;
    @Mock
    private HouseRepository houseRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private UtilityTypeRepository utilityTypeRepository;
    @Mock
    private RentalContractRepository contractRepository;
    @Mock
    private UtilityReadingMapper readingMapper;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private HouseAccessService houseAccessService;

    @InjectMocks
    private UtilityReadingServiceImpl utilityReadingService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private User owner() {
        return User.builder().id(10L).username("ownerA").passwordHash("h")
                .firstName("Owner").lastName("A").phone("099888777").email("owner@example.com")
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    private House house() {
        return House.builder().id(1L).owner(owner()).name("House A").code("HA-01")
                .address("Phnom Penh").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    private House otherOwnerHouse() {
        User other = User.builder().id(99L).username("other").passwordHash("h")
                .firstName("Other").lastName("Owner").phone("0111111111").email("other@example.com")
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        return House.builder().id(999L).owner(other).name("Other House").code("OH-01")
                .address("Kampong Cham").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    private Room roomA16() {
        Room room = new Room();
        room.setId(101L);
        room.setOwnerId(10L);
        room.setHouseId(1L);
        room.setRoomNumber("A16");
        room.setName("Room A16");
        room.setFloorNumber(2);
        return room;
    }

    private Room roomB12() {
        Room room = new Room();
        room.setId(102L);
        room.setOwnerId(10L);
        room.setHouseId(1L);
        room.setRoomNumber("B12");
        room.setName("Room B12");
        room.setFloorNumber(3);
        return room;
    }

    private UtilityType waterType() {
        return UtilityType.builder().id(1L).owner(owner()).name("Water").code("WATER")
                .unit("m\u00B3").build();
    }

    private UtilityType electricityType() {
        return UtilityType.builder().id(2L).owner(owner()).name("Electricity").code("ELECTRICITY")
                .unit("kWh").build();
    }

    private UtilityMeter waterMeter() {
        return UtilityMeter.builder().id(1001L).owner(owner()).houseId(1L).roomId(101L)
                .utilityTypeId(1L).meterNumber("16066131").status(UtilityMeterStatus.ACTIVE).build();
    }

    private UtilityMeter electricityMeter() {
        return UtilityMeter.builder().id(1002L).owner(owner()).houseId(1L).roomId(102L)
                .utilityTypeId(2L).meterNumber("1606424").status(UtilityMeterStatus.ACTIVE).build();
    }

    private UtilityReading reading(Long id, Long meterId, BigDecimal current, LocalDate date) {
        UtilityMeter meter = meterId == 1001L ? waterMeter() : electricityMeter();
        return UtilityReading.builder().id(id).meter(meter)
                .previousReading(BigDecimal.ZERO).currentReading(current)
                .usage(BigDecimal.ZERO).readingDate(date).build();
    }

    private void stubSheetBasics() {
        when(roomRepository.findByHouseIdIn(anyList())).thenReturn(List.of(roomA16(), roomB12()));
        when(houseRepository.findAllById(anyList())).thenReturn(List.of(house()));
        when(houseAccessService.getScopedOwnerIds()).thenReturn(List.of(10L));
        when(utilityTypeRepository.findByOwnerIdIn(anyList()))
                .thenReturn(List.of(waterType(), electricityType()));
        when(meterRepository.findByHouseIdIn(anyList()))
                .thenReturn(List.of(waterMeter(), electricityMeter()));
    }

    private void authenticateAsOwner() {
        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User("ownerA", "n/a", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    // ---- Monthly sheet ----

    @Test
    void getMonthlySheetReturnsRoomFloorHouseTenantAndPreservedCalculation() {
        when(securityUtils.getCurrentUserId()).thenReturn(10L);
        when(houseRepository.findById(1L)).thenReturn(Optional.of(house()));
        stubSheetBasics();

        when(contractRepository.findActiveTenantsByRoomIds(anyList(), anyList(), any(LocalDate.class)))
                .thenReturn(List.of(
                        new ActiveRoomTenantDto(101L, 7L, "Sok", "Dara", true),
                        new ActiveRoomTenantDto(102L, 8L, "Chan", "Nita", false)));
        when(readingRepository.findTopByMeterIdAndReadingDateBeforeOrderByReadingDateDesc(
                eq(1001L), any(LocalDate.class)))
                .thenReturn(Optional.of(reading(100L, 1001L, new BigDecimal("120"), LocalDate.of(2026, 8, 31))));
        when(readingRepository.findTopByMeterIdAndReadingDateBeforeOrderByReadingDateDesc(
                eq(1002L), any(LocalDate.class)))
                .thenReturn(Optional.of(reading(200L, 1002L, new BigDecimal("450"), LocalDate.of(2026, 8, 31))));
        when(readingRepository.findTopByMeterIdAndReadingDateBetweenOrderByReadingDateDesc(
                eq(1001L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Optional.of(UtilityReading.builder().id(999L).meter(waterMeter())
                        .previousReading(new BigDecimal("120")).currentReading(new BigDecimal("135"))
                        .usage(new BigDecimal("15")).readingDate(SEPT_2026).build()));

        List<MonthlyReadingRowResponse> rows =
                utilityReadingService.getMonthlySheet(1L, 2026, 9, null, null, null);

        assertThat(rows).hasSize(2);

        MonthlyReadingRowResponse water = rows.get(0);
        assertThat(water.roomId()).isEqualTo(101L);
        assertThat(water.roomNumber()).isEqualTo("A16");
        assertThat(water.floorNumber()).isEqualTo(2);
        assertThat(water.houseId()).isEqualTo(1L);
        assertThat(water.houseName()).isEqualTo("House A");
        assertThat(water.tenantName()).isEqualTo("Sok Dara");
        assertThat(water.meterId()).isEqualTo(1001L);
        assertThat(water.meterNumber()).isEqualTo("16066131");
        assertThat(water.utilityTypeCode()).isEqualTo("WATER");
        assertThat(water.previousReading()).isEqualByComparingTo("120");
        assertThat(water.currentReading()).isEqualByComparingTo("135");
        assertThat(water.usage()).isEqualByComparingTo("15");
        assertThat(water.readingDate()).isEqualTo(SEPT_2026);

        MonthlyReadingRowResponse electricity = rows.get(1);
        assertThat(electricity.roomNumber()).isEqualTo("B12");
        assertThat(electricity.floorNumber()).isEqualTo(3);
        assertThat(electricity.previousReading()).isEqualByComparingTo("450");
        assertThat(electricity.currentReading()).isNull();
        assertThat(electricity.usage()).isNull();
    }

    @Test
    void getMonthlySheetFiltersByFloorAndSearch() {
        when(securityUtils.getCurrentUserId()).thenReturn(10L);
        when(houseRepository.findById(1L)).thenReturn(Optional.of(house()));
        stubSheetBasics();

        when(contractRepository.findActiveTenantsByRoomIds(anyList(), anyList(), any(LocalDate.class)))
                .thenReturn(List.of(new ActiveRoomTenantDto(101L, 7L, "Sok", "Dara", true)));

        List<MonthlyReadingRowResponse> byFloor =
                utilityReadingService.getMonthlySheet(1L, 2026, 9, 2, null, null);
        List<MonthlyReadingRowResponse> searched =
                utilityReadingService.getMonthlySheet(1L, 2026, 9, null, "Sok Dara", null);
        List<MonthlyReadingRowResponse> noMatch =
                utilityReadingService.getMonthlySheet(1L, 2026, 9, 3, "A16", null);

        assertThat(byFloor).hasSize(1);
        assertThat(byFloor.get(0).roomNumber()).isEqualTo("A16");
        assertThat(searched).hasSize(1);
        assertThat(searched.get(0).roomNumber()).isEqualTo("A16");
        assertThat(searched.get(0).tenantName()).isEqualTo("Sok Dara");
        assertThat(noMatch).isEmpty();
    }

    @Test
    void getMonthlySheetFiltersByReadingStatus() {
        when(securityUtils.getCurrentUserId()).thenReturn(10L);
        when(houseRepository.findById(1L)).thenReturn(Optional.of(house()));
        stubSheetBasics();

        when(readingRepository.findTopByMeterIdAndReadingDateBetweenOrderByReadingDateDesc(
                eq(1001L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Optional.of(reading(11L, 1001L, new BigDecimal("135"), SEPT_2026)));

        List<MonthlyReadingRowResponse> entered = utilityReadingService
                .getMonthlySheet(1L, 2026, 9, null, null, ReadingStatusFilter.ENTERED);
        List<MonthlyReadingRowResponse> notEntered = utilityReadingService
                .getMonthlySheet(1L, 2026, 9, null, null, ReadingStatusFilter.NOT_ENTERED);

        assertThat(entered).hasSize(1);
        assertThat(entered.get(0).roomNumber()).isEqualTo("A16");
        assertThat(notEntered).hasSize(1);
        assertThat(notEntered.get(0).roomNumber()).isEqualTo("B12");
    }

    @Test
    void getMonthlySheetWithoutHouseUsesAccessibleScope() {
        when(houseAccessService.getAccessibleHouseIds()).thenReturn(List.of(1L));
        stubSheetBasics();

        List<MonthlyReadingRowResponse> rows =
                utilityReadingService.getMonthlySheet(null, 2026, 9, null, null, null);

        assertThat(rows).hasSize(2);
    }

    @Test
    void getMonthlySheetReturnsEmptyWhenNoAccessibleHouses() {
        when(houseAccessService.getAccessibleHouseIds()).thenReturn(List.of());

        List<MonthlyReadingRowResponse> rows =
                utilityReadingService.getMonthlySheet(null, 2026, 9, null, null, null);

        assertThat(rows).isEmpty();
    }

    @Test
    void getMonthlySheetThrowsForForeignHouse() {
        when(securityUtils.getCurrentUserId()).thenReturn(10L);
        when(houseRepository.findById(999L)).thenReturn(Optional.of(otherOwnerHouse()));
        when(userHouseAccessRepository.existsByUserIdAndHouseId(10L, 999L)).thenReturn(false);

        assertThatThrownBy(() -> utilityReadingService.getMonthlySheet(999L, 2026, 9, null, null, null))
                .isInstanceOf(HouseNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void getMonthlySheetFloorsReturnsDistinctSortedFloors() {
        when(securityUtils.getCurrentUserId()).thenReturn(10L);
        when(houseRepository.findById(1L)).thenReturn(Optional.of(house()));
        when(meterRepository.findByHouseIdIn(anyList()))
                .thenReturn(List.of(waterMeter(), electricityMeter()));
        when(roomRepository.findAllById(anyList())).thenReturn(List.of(roomB12(), roomA16()));

        List<Integer> floors = utilityReadingService.getMonthlySheetFloors(1L);

        assertThat(floors).containsExactly(2, 3);
    }

    // ---- Bulk save keeps calculation and validates ----

    @Test
    void bulkSaveReturnsTotalRowCountsAndPersistsOnlyOwnedMeters() {
        authenticateAsOwner();
        when(userRepository.findByUsername("ownerA")).thenReturn(Optional.of(owner()));
        when(securityUtils.getCurrentUserId()).thenReturn(10L);
        when(houseRepository.findById(1L)).thenReturn(Optional.of(house()));
        when(securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)).thenReturn(false);
        when(meterRepository.findByIdAndOwnerId(1001L, 10L)).thenReturn(Optional.of(waterMeter()));
        when(meterRepository.findByIdAndOwnerId(1002L, 10L)).thenReturn(Optional.of(electricityMeter()));
        when(readingRepository.findTopByMeterIdAndReadingDateBetweenOrderByReadingDateDesc(
                eq(1001L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Optional.of(reading(11L, 1001L, new BigDecimal("135"), SEPT_2026)));
        when(readingRepository.findTopByMeterIdAndReadingDateBetweenOrderByReadingDateDesc(
                eq(1002L), any(LocalDate.class), any(LocalDate.class))).thenReturn(Optional.empty());
        when(readingRepository.findTopByMeterIdAndReadingDateBeforeOrderByReadingDateDesc(
                eq(1001L), any(LocalDate.class)))
                .thenReturn(Optional.of(reading(10L, 1001L, new BigDecimal("120"), LocalDate.of(2026, 8, 31))));
        when(readingRepository.findTopByMeterIdAndReadingDateBeforeOrderByReadingDateDesc(
                eq(1002L), any(LocalDate.class))).thenReturn(Optional.empty());

        MonthlyReadingBulkRequest request = new MonthlyReadingBulkRequest(
                1L, 2026, 9, null,
                List.of(
                        new MonthlyReadingBulkRequest.MonthlyReadingRowRequest(1001L, new BigDecimal("150")),
                        new MonthlyReadingBulkRequest.MonthlyReadingRowRequest(1002L, new BigDecimal("500"))));

        BulkUtilityReadingResponse response = utilityReadingService.createReadingsBulk(request);

        assertThat(response.updatedCount()).isEqualTo(1);
        assertThat(response.createdCount()).isEqualTo(1);
        assertThat(response.skippedCount()).isZero();
        assertThat(response.createdCount() + response.updatedCount() + response.skippedCount()).isEqualTo(2);
        verify(readingRepository).saveAll(anyList());
    }

    @Test
    void bulkSaveSkipsUnchangedReadings() {
        authenticateAsOwner();
        when(userRepository.findByUsername("ownerA")).thenReturn(Optional.of(owner()));
        when(securityUtils.getCurrentUserId()).thenReturn(10L);
        when(houseRepository.findById(1L)).thenReturn(Optional.of(house()));
        when(securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)).thenReturn(false);
        when(meterRepository.findByIdAndOwnerId(1001L, 10L)).thenReturn(Optional.of(waterMeter()));
        when(readingRepository.findTopByMeterIdAndReadingDateBetweenOrderByReadingDateDesc(
                eq(1001L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Optional.of(reading(11L, 1001L, new BigDecimal("135"), SEPT_2026)));

        MonthlyReadingBulkRequest request = new MonthlyReadingBulkRequest(
                1L, 2026, 9, null,
                List.of(new MonthlyReadingBulkRequest.MonthlyReadingRowRequest(1001L, new BigDecimal("135"))));

        BulkUtilityReadingResponse response = utilityReadingService.createReadingsBulk(request);

        assertThat(response.updatedCount()).isZero();
        assertThat(response.createdCount()).isZero();
        assertThat(response.skippedCount()).isEqualTo(1);
        verify(readingRepository, never()).saveAll(anyList());
    }

    @Test
    void bulkSaveRejectsCurrentBelowPrevious() {
        authenticateAsOwner();
        when(userRepository.findByUsername("ownerA")).thenReturn(Optional.of(owner()));
        when(securityUtils.getCurrentUserId()).thenReturn(10L);
        when(houseRepository.findById(1L)).thenReturn(Optional.of(house()));
        when(securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)).thenReturn(false);
        when(meterRepository.findByIdAndOwnerId(1001L, 10L)).thenReturn(Optional.of(waterMeter()));
        when(readingRepository.findTopByMeterIdAndReadingDateBeforeOrderByReadingDateDesc(
                eq(1001L), any(LocalDate.class)))
                .thenReturn(Optional.of(reading(10L, 1001L, new BigDecimal("120"), LocalDate.of(2026, 8, 31))));

        MonthlyReadingBulkRequest request = new MonthlyReadingBulkRequest(
                1L, 2026, 9, null,
                List.of(new MonthlyReadingBulkRequest.MonthlyReadingRowRequest(1001L, new BigDecimal("100"))));

        assertThatThrownBy(() -> utilityReadingService.createReadingsBulk(request))
                .isInstanceOf(InvalidUtilityOperationException.class)
                .hasMessageContaining("previous");
        verify(readingRepository, never()).saveAll(anyList());
    }

    @Test
    void bulkSaveRejectsMeterOfAnotherHouse() {
        authenticateAsOwner();
        when(userRepository.findByUsername("ownerA")).thenReturn(Optional.of(owner()));
        when(securityUtils.getCurrentUserId()).thenReturn(10L);
        when(houseRepository.findById(2L)).thenReturn(Optional.of(house()));
        when(securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)).thenReturn(false);
        when(meterRepository.findByIdAndOwnerId(1002L, 10L)).thenReturn(Optional.of(electricityMeter()));

        MonthlyReadingBulkRequest request = new MonthlyReadingBulkRequest(
                2L, 2026, 9, null,
                List.of(new MonthlyReadingBulkRequest.MonthlyReadingRowRequest(1002L, new BigDecimal("10"))));

        assertThatThrownBy(() -> utilityReadingService.createReadingsBulk(request))
                .isInstanceOf(InvalidUtilityOperationException.class)
                .hasMessageContaining("belong");
        verify(readingRepository, never()).saveAll(anyList());
    }
}