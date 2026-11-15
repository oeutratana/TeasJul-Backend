package com.example.spring_boot_project_api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import com.example.spring_boot_project_api.dto.request.AdminBulkCreateRoomRequest;
import com.example.spring_boot_project_api.dto.request.AdminCreateRoomRequest;
import com.example.spring_boot_project_api.dto.request.CreateHouseRequest;
import com.example.spring_boot_project_api.dto.request.RoomTypeRequest;
import com.example.spring_boot_project_api.dto.response.HouseResponse;
import com.example.spring_boot_project_api.dto.response.RoomResponse;
import com.example.spring_boot_project_api.dto.response.RoomTypeResponse;
import com.example.spring_boot_project_api.enums.PropertyType;
import com.example.spring_boot_project_api.enums.RoomPublicationStatus;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.HouseNotFoundException;
import com.example.spring_boot_project_api.exception.RoomTypeNotFoundException;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.mapper.HouseMapper;
import com.example.spring_boot_project_api.mapper.RoomMapper;
import com.example.spring_boot_project_api.mapper.RoomTypeMapper;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.RoomType;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.RoomTypeRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.UserRoleRepository;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminRoomServiceImplTest {

    private static final long OWNER_ID = 42L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private HouseRepository houseRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomMapper roomMapper;

    @Mock
    private SecurityUtils securityUtils;

    @Spy
    private HouseMapper houseMapper = new HouseMapper();

    @Spy
    private RoomTypeMapper roomTypeMapper = new RoomTypeMapper();

    @InjectMocks
    private AdminRoomServiceImpl adminRoomService;

    @BeforeEach
    void setUp() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
        when(securityUtils.getCurrentUserId()).thenReturn(99L);
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner()));
        when(userRoleRepository.existsByUserIdAndRoleCode(OWNER_ID, SecurityUtils.ROLE_OWNER))
                .thenReturn(true);
    }

    private User owner() {
        return User.builder().id(OWNER_ID).username("owner1").build();
    }

    private House house() {
        return House.builder().id(7L).name("Villa Mondul").slug("villa-mondul")
                .code("H001").address("Phnom Penh").owner(owner()).build();
    }

    private RoomType roomType() {
        RoomType roomType = new RoomType();
        roomType.setId(3L);
        roomType.setOwnerId(OWNER_ID);
        roomType.setName("Deluxe");
        roomType.setCode("DELUXE");
        return roomType;
    }

    private AdminCreateRoomRequest validRequest() {
        AdminCreateRoomRequest request = new AdminCreateRoomRequest();
        request.setHouseId(7L);
        request.setRoomTypeId(3L);
        request.setRoomNumber("101");
        request.setName("Sunset Room");
        request.setCapacity(2);
        request.setMonthlyRent(BigDecimal.valueOf(350));
        return request;
    }

    private void allowCreate() {
        RoomType roomType = roomType();
        when(houseRepository.findByIdAndOwnerId(7L, OWNER_ID)).thenReturn(Optional.of(house()));
        when(roomTypeRepository.findById(3L)).thenReturn(Optional.of(roomType));
        when(roomRepository.existsByHouseIdAndRoomNumber(7L, "101")).thenReturn(false);
    }

    // ---------- getOwnerHouses ----------

    @Test
    @DisplayName("listOwnerHouses returns mapped houses of the owner")
    void getOwnerHousesReturnsMappedHouses() {
        when(houseRepository.findAllByOwnerIdOrderByCreatedAtDesc(OWNER_ID))
                .thenReturn(List.of(house()));

        List<HouseResponse> result = adminRoomService.getOwnerHouses(OWNER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Villa Mondul");
        assertThat(result.get(0).ownerId()).isEqualTo(OWNER_ID);
    }

    @Test
    @DisplayName("listOwnerHouses rejects a non-super-admin")
    void getOwnerHousesRejectsNonSuperAdmin() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> adminRoomService.getOwnerHouses(OWNER_ID))
                .isInstanceOf(AccessDeniedException.class);

        verify(houseRepository, never()).findAllByOwnerIdOrderByCreatedAtDesc(any());
    }

    // ---------- getOwnerRoomTypes ----------

    @Test
    @DisplayName("listOwnerRoomTypes returns mapped room types of the owner")
    void getOwnerRoomTypesReturnsMappedRoomTypes() {
        when(roomTypeRepository.findByOwnerId(OWNER_ID)).thenReturn(List.of(roomType()));

        List<RoomTypeResponse> result = adminRoomService.getOwnerRoomTypes(OWNER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Deluxe");
        assertThat(result.get(0).getOwnerId()).isEqualTo(OWNER_ID);
    }

    @Test
    @DisplayName("listOwnerRoomTypes rejects a non-super-admin")
    void getOwnerRoomTypesRejectsNonSuperAdmin() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> adminRoomService.getOwnerRoomTypes(OWNER_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ---------- createRoomForOwner ----------

    @Test
    @DisplayName("createRoom creates a DRAFT room for the owner by default")
    void createRoomCreatesDraftByDefault() {
        allowCreate();
        when(roomMapper.toEntity(any(AdminCreateRoomRequest.class))).thenReturn(new Room());
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roomMapper.toDto(any(Room.class))).thenReturn(new RoomResponse());

        adminRoomService.createRoomForOwner(OWNER_ID, validRequest());

        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(captor.capture());
        assertThat(captor.getValue().getOwnerId()).isEqualTo(OWNER_ID);
        assertThat(captor.getValue().getPublicationStatus())
                .isEqualTo(RoomPublicationStatus.DRAFT);
        assertThat(captor.getValue().getApprovedAt()).isNull();
    }

    @Test
    @DisplayName("createRoom publishes immediately when requested")
    void createRoomPublishesImmediatelyWhenRequested() {
        allowCreate();
        AdminCreateRoomRequest request = validRequest();
        request.setPublishImmediately(true);
        when(roomMapper.toEntity(any(AdminCreateRoomRequest.class))).thenReturn(new Room());
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roomMapper.toDto(any(Room.class))).thenReturn(new RoomResponse());

        adminRoomService.createRoomForOwner(OWNER_ID, request);

        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(captor.capture());
        assertThat(captor.getValue().getOwnerId()).isEqualTo(OWNER_ID);
        assertThat(captor.getValue().getPublicationStatus())
                .isEqualTo(RoomPublicationStatus.PUBLISHED);
        assertThat(captor.getValue().getApprovedAt()).isNotNull();
        assertThat(captor.getValue().getApprovedBy()).isEqualTo(99L);
        assertThat(captor.getValue().getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("createRoom rejects a non-super-admin")
    void createRoomRejectsNonSuperAdmin() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> adminRoomService.createRoomForOwner(OWNER_ID, validRequest()))
                .isInstanceOf(AccessDeniedException.class);

        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    @DisplayName("createRoom throws 404 when the owner does not exist")
    void createRoomRejectsUnknownOwner() {
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminRoomService.createRoomForOwner(OWNER_ID, validRequest()))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("createRoom throws 404 when the user is not an owner")
    void createRoomRejectsNonOwnerUser() {
        when(userRoleRepository.existsByUserIdAndRoleCode(OWNER_ID, SecurityUtils.ROLE_OWNER))
                .thenReturn(false);

        assertThatThrownBy(() -> adminRoomService.createRoomForOwner(OWNER_ID, validRequest()))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("createRoom throws 404 when the house is not owned by the owner")
    void createRoomRejectsForeignHouse() {
        when(houseRepository.findByIdAndOwnerId(7L, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminRoomService.createRoomForOwner(OWNER_ID, validRequest()))
                .isInstanceOf(HouseNotFoundException.class);
    }

    @Test
    @DisplayName("createRoom throws 404 when the room type is not owned by the owner")
    void createRoomRejectsForeignRoomType() {
        House house = house();
        RoomType foreign = roomType();
        foreign.setOwnerId(999L);
        when(houseRepository.findByIdAndOwnerId(7L, OWNER_ID)).thenReturn(Optional.of(house));
        when(roomTypeRepository.findById(3L)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> adminRoomService.createRoomForOwner(OWNER_ID, validRequest()))
                .isInstanceOf(RoomTypeNotFoundException.class);
    }

    @Test
    @DisplayName("createRoom rejects a duplicate room number in the house")
    void createRoomRejectsDuplicateRoomNumber() {
        allowCreate();
        when(roomRepository.existsByHouseIdAndRoomNumber(7L, "101")).thenReturn(true);

        assertThatThrownBy(() -> adminRoomService.createRoomForOwner(OWNER_ID, validRequest()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("101");

        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    @DisplayName("createRoom rejects a ROOM listing without a room number")
    void createRoomRejectsRoomWithoutRoomNumber() {
        allowCreate();
        AdminCreateRoomRequest request = validRequest();
        request.setRoomNumber(null);

        assertThatThrownBy(() -> adminRoomService.createRoomForOwner(OWNER_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Room number");

        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    @DisplayName("createRoom rejects a ROOM listing without capacity")
    void createRoomRejectsRoomWithoutCapacity() {
        allowCreate();
        AdminCreateRoomRequest request = validRequest();
        request.setCapacity(null);

        assertThatThrownBy(() -> adminRoomService.createRoomForOwner(OWNER_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Capacity");

        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    @DisplayName("createRoom saves a HOUSE listing without any room identity fields")
    void createRoomSavesStructureWithoutRoomIdentity() {
        when(houseRepository.findByIdAndOwnerId(7L, OWNER_ID)).thenReturn(Optional.of(house()));
        when(roomMapper.toEntity(any(AdminCreateRoomRequest.class))).thenReturn(new Room());
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roomMapper.toDto(any(Room.class))).thenReturn(new RoomResponse());

        AdminCreateRoomRequest request = validRequest();
        request.setPropertyType(PropertyType.HOUSE);
        request.setRoomTypeId(null);
        request.setRoomNumber(null);
        request.setFloorNumber(null);
        request.setCapacity(null);

        adminRoomService.createRoomForOwner(OWNER_ID, request);

        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(captor.capture());
        assertThat(captor.getValue().getOwnerId()).isEqualTo(OWNER_ID);
    }

    @Test
    @DisplayName("createRoom normalises room identity fields away for structural listings")
    void createRoomNormalisesRoomIdentityForStructure() {
        when(houseRepository.findByIdAndOwnerId(7L, OWNER_ID)).thenReturn(Optional.of(house()));
        when(roomMapper.toEntity(any(AdminCreateRoomRequest.class))).thenReturn(new Room());
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roomMapper.toDto(any(Room.class))).thenReturn(new RoomResponse());

        AdminCreateRoomRequest request = validRequest();
        request.setPropertyType(PropertyType.VILLA);

        adminRoomService.createRoomForOwner(OWNER_ID, request);

        assertThat(request.getRoomTypeId()).isNull();
        assertThat(request.getRoomNumber()).isNull();
        assertThat(request.getFloorNumber()).isNull();
    }

    @Test
    @DisplayName("createRoom requires unit number and floor for APARTMENT/CONDO listings")
    void createRoomUnitRequiresUnitNumberAndFloor() {
        allowCreate();
        AdminCreateRoomRequest request = validRequest();
        request.setPropertyType(PropertyType.APARTMENT);
        request.setRoomNumber(null);
        request.setFloorNumber(null);
        request.setRoomTypeId(null);

        assertThatThrownBy(() -> adminRoomService.createRoomForOwner(OWNER_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unit number");

        verify(roomRepository, never()).save(any(Room.class));
    }

    // ---------- createRoomsForOwner ----------

    private AdminBulkCreateRoomRequest bulkRequest(String... roomNumbers) {
        AdminBulkCreateRoomRequest request = new AdminBulkCreateRoomRequest();
        request.setRooms(List.of(roomNumbers).stream().map(number -> {
            AdminCreateRoomRequest entry = validRequest();
            entry.setRoomNumber(number);
            return entry;
        }).toList());
        return request;
    }

    private void allowBulkCreate(String... roomNumbers) {
        allowCreate();
        for (String number : roomNumbers) {
            when(roomRepository.existsByHouseIdAndRoomNumber(7L, number)).thenReturn(false);
        }
        when(roomMapper.toEntity(any(AdminCreateRoomRequest.class))).thenReturn(new Room());
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roomMapper.toDto(any(Room.class))).thenReturn(new RoomResponse());
    }

    @Test
    @DisplayName("createRooms creates one DRAFT room per entry for the owner")
    void createRoomsCreatesEachRoomForOwner() {
        allowBulkCreate("101", "102");
        when(roomRepository.existsByHouseIdAndRoomNumber(7L, "101")).thenReturn(false);
        when(roomRepository.existsByHouseIdAndRoomNumber(7L, "102")).thenReturn(false);

        List<RoomResponse> result = adminRoomService.createRoomsForOwner(OWNER_ID, bulkRequest("101", "102"));

        assertThat(result).hasSize(2);
        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Room::getOwnerId)
                .containsExactly(OWNER_ID, OWNER_ID);
    }

    @Test
    @DisplayName("createRooms rejects a non-super-admin")
    void createRoomsRejectsNonSuperAdmin() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> adminRoomService.createRoomsForOwner(OWNER_ID, bulkRequest("101")))
                .isInstanceOf(AccessDeniedException.class);

        verify(roomRepository, never()).save(any(Room.class));
    }

    // ---------- createHouseForOwner ----------

    private CreateHouseRequest houseRequest() {
        return new CreateHouseRequest(
                "Villa Mondul", "villa-mondul", "H001", "Phnom Penh",
                null, null, null, null, null,
                5, 3, "Nice villa", null, null, null, null,
                2, 1, null, null, null);
    }

    @Test
    @DisplayName("createHouse creates a house owned by the selected owner")
    void createHouseCreatesHouseForOwner() {
        when(houseRepository.existsByOwnerIdAndName(OWNER_ID, "Villa Mondul")).thenReturn(false);
        when(houseRepository.existsByOwnerIdAndSlug(OWNER_ID, "villa-mondul")).thenReturn(false);
        when(houseRepository.existsByOwnerIdAndCode(OWNER_ID, "H001")).thenReturn(false);
        when(houseRepository.save(any(House.class))).thenAnswer(inv -> inv.getArgument(0));

        HouseResponse result = adminRoomService.createHouseForOwner(OWNER_ID, houseRequest());

        assertThat(result.name()).isEqualTo("Villa Mondul");
        assertThat(result.ownerId()).isEqualTo(OWNER_ID);
        ArgumentCaptor<House> captor = ArgumentCaptor.forClass(House.class);
        verify(houseRepository).save(captor.capture());
        assertThat(captor.getValue().getOwner().getId()).isEqualTo(OWNER_ID);
    }

    @Test
    @DisplayName("createHouse rejects a duplicate name for the owner")
    void createHouseRejectsDuplicateName() {
        when(houseRepository.existsByOwnerIdAndName(OWNER_ID, "Villa Mondul")).thenReturn(true);

        assertThatThrownBy(() -> adminRoomService.createHouseForOwner(OWNER_ID, houseRequest()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Villa Mondul");

        verify(houseRepository, never()).save(any(House.class));
    }

    @Test
    @DisplayName("createHouse rejects a non-super-admin")
    void createHouseRejectsNonSuperAdmin() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> adminRoomService.createHouseForOwner(OWNER_ID, houseRequest()))
                .isInstanceOf(AccessDeniedException.class);

        verify(houseRepository, never()).save(any(House.class));
    }

    // ---------- createRoomTypeForOwner ----------

    private RoomTypeRequest roomTypeRequest() {
        RoomTypeRequest request = new RoomTypeRequest();
        request.setName("Deluxe");
        request.setCode("DELUXE");
        request.setCapacity(2);
        request.setDefaultRent(BigDecimal.valueOf(350));
        request.setDefaultDeposit(BigDecimal.valueOf(500));
        return request;
    }

    @Test
    @DisplayName("createRoomType creates a room type owned by the selected owner")
    void createRoomTypeCreatesRoomTypeForOwner() {
        when(roomTypeRepository.existsByCode("DELUXE")).thenReturn(false);
        when(roomTypeRepository.save(any(RoomType.class))).thenAnswer(inv -> inv.getArgument(0));

        RoomTypeResponse result = adminRoomService.createRoomTypeForOwner(OWNER_ID, roomTypeRequest());

        assertThat(result.getName()).isEqualTo("Deluxe");
        assertThat(result.getOwnerId()).isEqualTo(OWNER_ID);
        ArgumentCaptor<RoomType> captor = ArgumentCaptor.forClass(RoomType.class);
        verify(roomTypeRepository).save(captor.capture());
        assertThat(captor.getValue().getOwnerId()).isEqualTo(OWNER_ID);
    }

    @Test
    @DisplayName("createRoomType rejects a duplicate code")
    void createRoomTypeRejectsDuplicateCode() {
        when(roomTypeRepository.existsByCode("DELUXE")).thenReturn(true);

        assertThatThrownBy(() -> adminRoomService.createRoomTypeForOwner(OWNER_ID, roomTypeRequest()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("DELUXE");

        verify(roomTypeRepository, never()).save(any(RoomType.class));
    }

    @Test
    @DisplayName("createRoomType rejects a non-super-admin")
    void createRoomTypeRejectsNonSuperAdmin() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> adminRoomService.createRoomTypeForOwner(OWNER_ID, roomTypeRequest()))
                .isInstanceOf(AccessDeniedException.class);

        verify(roomTypeRepository, never()).save(any(RoomType.class));
    }

    @Test
    @DisplayName("createRoomType throws 404 when the owner does not exist")
    void createRoomTypeRejectsUnknownOwner() {
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminRoomService.createRoomTypeForOwner(OWNER_ID, roomTypeRequest()))
                .isInstanceOf(UserNotFoundException.class);
    }
}