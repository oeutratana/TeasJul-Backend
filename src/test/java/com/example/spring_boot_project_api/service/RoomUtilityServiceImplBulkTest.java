package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.example.spring_boot_project_api.dto.request.BulkRoomUtilityRequest;
import com.example.spring_boot_project_api.dto.response.BulkAssignmentResponse;
import com.example.spring_boot_project_api.enums.UtilityBillingMethod;
import com.example.spring_boot_project_api.enums.UtilityTypeStatus;
import com.example.spring_boot_project_api.mapper.RoomUtilityMapper;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.RoomUtility;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UtilityType;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.RoomUtilityRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.UtilityTypeRepository;
import com.example.spring_boot_project_api.service.impl.RoomUtilityServiceImpl;
import com.example.spring_boot_project_api.util.PermissionConstants;
import com.example.spring_boot_project_api.util.SecurityUtils;

class RoomUtilityServiceImplBulkTest {

    private final RoomUtilityRepository roomUtilityRepository = mock(RoomUtilityRepository.class);
    private final RoomRepository roomRepository = mock(RoomRepository.class);
    private final HouseRepository houseRepository = mock(HouseRepository.class);
    private final UtilityTypeRepository utilityTypeRepository = mock(UtilityTypeRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserHouseAccessRepository accessRepository = mock(UserHouseAccessRepository.class);
    private final SecurityUtils securityUtils = mock(SecurityUtils.class);
    private RoomUtilityServiceImpl service;

    private User owner;
    private UtilityType electricity;
    private UtilityType water;
    private Room roomA;
    private Room roomB;

    @BeforeEach
    void setUp() {
        service = new RoomUtilityServiceImpl(roomUtilityRepository, roomRepository, houseRepository,
                utilityTypeRepository, userRepository, accessRepository, new RoomUtilityMapper(), securityUtils);

        owner = User.builder().id(1L).build();
        House ownerHouse = House.builder().id(100L).owner(owner).build();

        roomA = room(1L, 100L, "A-1");
        roomB = room(2L, 100L, "A-2");

        electricity = UtilityType.builder().id(30L).name("Electricity").code("ELEC")
                .unit("kWh").billingMethod(UtilityBillingMethod.METERED)
                .status(UtilityTypeStatus.ACTIVE).build();
        water = UtilityType.builder().id(31L).name("Water").code("WATER")
                .unit("m3").billingMethod(UtilityBillingMethod.METERED)
                .status(UtilityTypeStatus.ACTIVE).build();

        when(roomRepository.findAllById(any())).thenReturn(List.of(roomA, roomB));
        when(houseRepository.findById(100L)).thenReturn(Optional.of(ownerHouse));
        when(utilityTypeRepository.findByIdAndOwnerId(30L, 1L)).thenReturn(Optional.of(electricity));
        when(utilityTypeRepository.findByIdAndOwnerId(31L, 1L)).thenReturn(Optional.of(water));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
    }

    private Room room(Long id, Long houseId, String roomNumber) {
        Room room = new Room();
        room.setId(id);
        room.setHouseId(houseId);
        room.setRoomNumber(roomNumber);
        return room;
    }

    private RoomUtility assignment(Long roomId, Long typeId, UtilityTypeStatus status) {
        return RoomUtility.builder().id(roomId * 100 + typeId).owner(owner)
                .roomId(roomId).utilityTypeId(typeId).status(status).isIncluded(true).build();
    }

    private BulkRoomUtilityRequest request(List<Long> roomIds, List<Long> typeIds, Boolean isIncluded) {
        return new BulkRoomUtilityRequest(roomIds, typeIds, isIncluded);
    }

    @Test
    void createsAssignmentsForAllRoomsAndAllTypes() {
        BulkAssignmentResponse response = service.bulkAssignUtilities(
                request(List.of(1L, 2L), List.of(30L, 31L), true));

        assertThat(response.roomsProcessed()).isEqualTo(2);
        assertThat(response.assignmentsCreated()).isEqualTo(4);
        assertThat(response.alreadyAssigned()).isZero();
        assertThat(response.failures()).isEmpty();
        verify(roomUtilityRepository, times(4)).save(any(RoomUtility.class));
    }

    @Test
    void skipsExistingActiveAssignments() {
        when(roomUtilityRepository.findByRoomIdIn(List.of(1L, 2L)))
                .thenReturn(List.of(assignment(1L, 30L, UtilityTypeStatus.ACTIVE)));

        BulkAssignmentResponse response = service.bulkAssignUtilities(
                request(List.of(1L, 2L), List.of(30L, 31L), null));

        assertThat(response.alreadyAssigned()).isEqualTo(1);
        assertThat(response.assignmentsCreated()).isEqualTo(3);
        verify(roomUtilityRepository, times(3)).save(any(RoomUtility.class));
    }

    @Test
    void reactivatesInactiveAssignmentsAndCountsThemAsCreated() {
        RoomUtility inactive = assignment(1L, 30L, UtilityTypeStatus.INACTIVE);
        when(roomUtilityRepository.findByRoomIdIn(List.of(1L, 2L))).thenReturn(List.of(inactive));

        BulkAssignmentResponse response = service.bulkAssignUtilities(
                request(List.of(1L, 2L), List.of(30L, 31L), null));

        assertThat(response.assignmentsCreated()).isEqualTo(4);
        assertThat(response.alreadyAssigned()).isZero();
        assertThat(inactive.getStatus()).isEqualTo(UtilityTypeStatus.ACTIVE);
        verify(roomUtilityRepository, times(4)).save(any(RoomUtility.class));
    }

    @Test
    void collectsMissingRoomAndForeignTypeFailures() {
        when(roomRepository.findAllById(List.of(1L, 999L))).thenReturn(List.of(roomA));
        when(utilityTypeRepository.findByIdAndOwnerId(44L, 1L)).thenReturn(Optional.empty());

        BulkAssignmentResponse response = service.bulkAssignUtilities(
                request(List.of(1L, 999L), List.of(30L, 44L), null));

        assertThat(response.roomsProcessed()).isEqualTo(1);
        assertThat(response.assignmentsCreated()).isEqualTo(1);
        assertThat(response.failures()).hasSize(3);
        assertThat(response.failures()).filteredOn(f -> f.roomId().equals(999L)).hasSize(2);
        assertThat(response.failures()).anySatisfy(f -> {
            assertThat(f.entityId()).isEqualTo(44L);
            assertThat(f.reason()).contains("does not belong to the house owner");
        });
    }

    @Test
    void rejectsRoomsOutsideAccess() {
        Room foreign = room(8L, 200L, "F-1");
        when(roomRepository.findAllById(List.of(8L))).thenReturn(List.of(foreign));
        when(houseRepository.findById(200L)).thenReturn(
                Optional.of(House.builder().id(200L).owner(User.builder().id(2L).build()).build()));

        assertThatThrownBy(() -> service.bulkAssignUtilities(
                request(List.of(8L), List.of(30L), null)))
                .isInstanceOf(AccessDeniedException.class);
        verify(roomUtilityRepository, never()).save(any(RoomUtility.class));
    }

    @Test
    void superAdminCanAssignToForeignHouse() {
        Room foreign = room(8L, 200L, "F-1");
        when(roomRepository.findAllById(List.of(8L))).thenReturn(List.of(foreign));
        when(houseRepository.findById(200L)).thenReturn(
                Optional.of(House.builder().id(200L).owner(User.builder().id(2L).build()).build()));
        when(utilityTypeRepository.findByIdAndOwnerId(30L, 2L)).thenReturn(Optional.of(electricity));
        when(utilityTypeRepository.findByIdAndOwnerId(31L, 2L)).thenReturn(Optional.of(water));
        when(userRepository.findById(2L)).thenReturn(Optional.of(User.builder().id(2L).build()));
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);

        BulkAssignmentResponse response = service.bulkAssignUtilities(
                request(List.of(8L), List.of(30L, 31L), null));

        assertThat(response.assignmentsCreated()).isEqualTo(2);
        assertThat(response.failures()).isEmpty();
    }

    @Test
    void managerWithHouseAccessCanAssignForHouseOwner() {
        when(securityUtils.getCurrentUserId()).thenReturn(3L);
        when(securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)).thenReturn(true);
        when(accessRepository.existsByUserIdAndHouseId(3L, 100L)).thenReturn(true);

        BulkAssignmentResponse response = service.bulkAssignUtilities(
                request(List.of(1L, 2L), List.of(30L), null));

        assertThat(response.assignmentsCreated()).isEqualTo(2);
        assertThat(response.failures()).isEmpty();
    }

    @Test
    void deduplicatesRequestedIds() {
        service.bulkAssignUtilities(request(List.of(1L, 1L, 2L), List.of(30L, 30L), null));

        verify(roomUtilityRepository, times(2)).save(any(RoomUtility.class));
    }

    @Test
    void rejectsEmptyRoomsOrTypes() {
        assertThatThrownBy(() -> service.bulkAssignUtilities(request(List.of(), List.of(30L), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one room");
        assertThatThrownBy(() -> service.bulkAssignUtilities(request(List.of(1L), List.of(), null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.bulkAssignUtilities(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requiresUtilityManagePermission() {
        doThrow(new AccessDeniedException("denied"))
                .when(securityUtils).requirePermission(PermissionConstants.UTILITY_MANAGE);

        assertThatThrownBy(() -> service.bulkAssignUtilities(
                request(List.of(1L), List.of(30L), null)))
                .isInstanceOf(AccessDeniedException.class);
        verify(roomUtilityRepository, never()).save(any(RoomUtility.class));
    }
}