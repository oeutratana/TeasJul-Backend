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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.spring_boot_project_api.dto.request.BulkRoomAmenityRequest;
import com.example.spring_boot_project_api.dto.response.BulkAssignmentResponse;
import com.example.spring_boot_project_api.mapper.RoomAmenityMapper;
import com.example.spring_boot_project_api.model.Amenity;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.RoomAmenity;
import com.example.spring_boot_project_api.model.RoomAmenityId;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.AmenityRepository;
import com.example.spring_boot_project_api.repository.RoomAmenityRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.impl.RoomAmenityServiceImpl;
import com.example.spring_boot_project_api.util.SecurityUtils;

class RoomAmenityServiceImplBulkTest {

    private final RoomAmenityRepository roomAmenityRepository = mock(RoomAmenityRepository.class);
    private final AmenityRepository amenityRepository = mock(AmenityRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final RoomRepository roomRepository = mock(RoomRepository.class);
    private final HouseAccessService houseAccessService = mock(HouseAccessService.class);
    private final SecurityUtils securityUtils = mock(SecurityUtils.class);
    private RoomAmenityServiceImpl service;

    private User owner;
    private Amenity wifi;
    private Amenity parking;
    private Room roomA;
    private Room roomB;

    @BeforeEach
    void setUp() {
        service = new RoomAmenityServiceImpl(roomAmenityRepository, amenityRepository,
                userRepository, roomRepository, new RoomAmenityMapper(),
                houseAccessService, securityUtils);

        owner = User.builder().id(1L).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new org.springframework.security.core.userdetails.User(
                                "owner@example.com", "pw", List.of()),
                        null, List.of()));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));

        roomA = room(1L, 100L);
        roomB = room(2L, 100L);
        wifi = amenity(50L, 1L, "WiFi");
        parking = amenity(51L, 1L, "Parking");

        when(roomRepository.findAllById(any())).thenReturn(List.of(roomA, roomB));
        when(amenityRepository.findById(50L)).thenReturn(Optional.of(wifi));
        when(amenityRepository.findById(51L)).thenReturn(Optional.of(parking));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Amenity amenity(Long id, Long ownerId, String name) {
        return Amenity.builder().id(id).ownerId(ownerId).name(name)
                .code(name.toUpperCase()).build();
    }

    private Room room(Long id, Long houseId) {
        Room room = new Room();
        room.setId(id);
        room.setHouseId(houseId);
        return room;
    }

    private BulkRoomAmenityRequest request(List<Long> roomIds, List<Long> amenityIds) {
        return new BulkRoomAmenityRequest(roomIds, amenityIds);
    }

    @Test
    void createsAssignmentsForAllRoomsAndAllAmenities() {
        BulkAssignmentResponse response = service.bulkAssignAmenities(
                request(List.of(1L, 2L), List.of(50L, 51L)));

        assertThat(response.roomsProcessed()).isEqualTo(2);
        assertThat(response.assignmentsCreated()).isEqualTo(4);
        assertThat(response.alreadyAssigned()).isZero();
        assertThat(response.failures()).isEmpty();
        verify(roomAmenityRepository, times(4)).save(any(RoomAmenity.class));
        verify(houseAccessService, times(2)).requireHouseAccess(100L);
    }

    @Test
    void skipsExistingAssignments() {
        when(roomAmenityRepository.findByIdRoomIdIn(List.of(1L, 2L)))
                .thenReturn(List.of(RoomAmenity.builder()
                        .id(new RoomAmenityId(1L, 50L))
                        .amenity(wifi)
                        .build()));

        BulkAssignmentResponse response = service.bulkAssignAmenities(
                request(List.of(1L, 2L), List.of(50L, 51L)));

        assertThat(response.alreadyAssigned()).isEqualTo(1);
        assertThat(response.assignmentsCreated()).isEqualTo(3);
        verify(roomAmenityRepository, times(3)).save(any(RoomAmenity.class));
    }

    @Test
    void collectsMissingAmenityAndMissingRoomFailures() {
        when(roomRepository.findAllById(List.of(1L, 999L))).thenReturn(List.of(roomA));
        when(amenityRepository.findById(99L)).thenReturn(Optional.empty());

        BulkAssignmentResponse response = service.bulkAssignAmenities(
                request(List.of(1L, 999L), List.of(50L, 99L)));

        assertThat(response.roomsProcessed()).isEqualTo(1);
        assertThat(response.assignmentsCreated()).isEqualTo(1);
        assertThat(response.failures()).hasSize(3);
        assertThat(response.failures()).filteredOn(f -> f.roomId().equals(999L)).hasSize(2);
        assertThat(response.failures()).anySatisfy(f -> {
            assertThat(f.entityId()).isEqualTo(99L);
            assertThat(f.reason()).contains("Amenity not found");
        });
    }

    @Test
    void rejectsForeignAmenity() {
        Amenity foreign = amenity(90L, 9L, "Foreign");
        when(amenityRepository.findById(90L)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.bulkAssignAmenities(
                request(List.of(1L), List.of(90L))))
                .isInstanceOf(AccessDeniedException.class);
        verify(roomAmenityRepository, never()).save(any(RoomAmenity.class));
    }

    @Test
    void superAdminCanAssignForeignAmenity() {
        Amenity foreign = amenity(90L, 9L, "Foreign");
        when(amenityRepository.findById(90L)).thenReturn(Optional.of(foreign));
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);

        BulkAssignmentResponse response = service.bulkAssignAmenities(
                request(List.of(1L), List.of(90L)));

        assertThat(response.assignmentsCreated()).isEqualTo(1);
        assertThat(response.failures()).isEmpty();
    }

    @Test
    void managerCanAssignAmenityOwnedByManagedOwner() {
        Amenity managed = amenity(90L, 9L, "Managed");
        when(amenityRepository.findById(90L)).thenReturn(Optional.of(managed));
        when(houseAccessService.getScopedOwnerIds()).thenReturn(List.of(9L));

        BulkAssignmentResponse response = service.bulkAssignAmenities(
                request(List.of(1L), List.of(90L)));

        assertThat(response.assignmentsCreated()).isEqualTo(1);
        assertThat(response.failures()).isEmpty();
    }

    @Test
    void rejectsRoomOutsideAccess() {
        Room foreign = room(8L, 200L);
        when(roomRepository.findAllById(List.of(8L))).thenReturn(List.of(foreign));
        doThrow(new AccessDeniedException("denied"))
                .when(houseAccessService).requireHouseAccess(200L);

        assertThatThrownBy(() -> service.bulkAssignAmenities(
                request(List.of(8L), List.of(50L))))
                .isInstanceOf(AccessDeniedException.class);
        verify(roomAmenityRepository, never()).save(any(RoomAmenity.class));
    }

    @Test
    void deduplicatesRequestedIds() {
        service.bulkAssignAmenities(request(List.of(1L, 1L, 2L), List.of(50L, 50L)));

        verify(roomAmenityRepository, times(2)).save(any(RoomAmenity.class));
    }

    @Test
    void rejectsEmptyRoomsOrAmenities() {
        assertThatThrownBy(() -> service.bulkAssignAmenities(request(List.of(), List.of(50L))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one room");
        assertThatThrownBy(() -> service.bulkAssignAmenities(request(List.of(1L), List.of())))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.bulkAssignAmenities(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}