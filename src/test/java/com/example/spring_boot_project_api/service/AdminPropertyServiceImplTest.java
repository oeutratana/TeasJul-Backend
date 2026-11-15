package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import com.example.spring_boot_project_api.dto.request.AdminPropertyHouseRequest;
import com.example.spring_boot_project_api.dto.request.AdminPropertyPostRequest;
import com.example.spring_boot_project_api.dto.request.PropertyOwnerRequest;
import com.example.spring_boot_project_api.dto.response.AdminPropertyResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.PropertyOwnerResponse;
import com.example.spring_boot_project_api.enums.PropertyType;
import com.example.spring_boot_project_api.enums.RoomPublicationStatus;
import com.example.spring_boot_project_api.exception.PropertyOwnerNotFoundException;
import com.example.spring_boot_project_api.mapper.PropertyOwnerMapper;
import com.example.spring_boot_project_api.mapper.UserMapper;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.PropertyOwner;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserStatus;
import com.example.spring_boot_project_api.repository.AmenityRepository;
import com.example.spring_boot_project_api.repository.HouseImageRepository;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.NearbyLocationRepository;
import com.example.spring_boot_project_api.repository.PropertyOwnerRepository;
import com.example.spring_boot_project_api.repository.RoomAmenityRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.RoomTypeRepository;
import com.example.spring_boot_project_api.repository.RoomUtilityRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.UserRoleRepository;
import com.example.spring_boot_project_api.repository.UtilityRateRepository;
import com.example.spring_boot_project_api.repository.UtilityTypeRepository;
import com.example.spring_boot_project_api.service.impl.AdminPropertyServiceImpl;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
class AdminPropertyServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Spy
    private UserMapper userMapper = new UserMapper();

    @Mock
    private HouseRepository houseRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private PropertyOwnerRepository propertyOwnerRepository;

    @Spy
    private PropertyOwnerMapper propertyOwnerMapper = new PropertyOwnerMapper();

    @Mock
    private AmenityRepository amenityRepository;

    @Mock
    private RoomAmenityRepository roomAmenityRepository;

    @Mock
    private RoomUtilityRepository roomUtilityRepository;

    @Mock
    private UtilityTypeRepository utilityTypeRepository;

    @Mock
    private UtilityRateRepository utilityRateRepository;

    @Mock
    private NearbyLocationRepository nearbyLocationRepository;

    @Mock
    private HouseImageRepository houseImageRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private AdminPropertyServiceImpl adminPropertyService;

    private void allowSuperAdmin() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
    }

    private User ownerEntity() {
        return User.builder()
                .id(1L)
                .username("mao.leng")
                .email("mao.leng@rental.com")
                .firstName("Mao")
                .lastName("Leng")
                .phone("+85512345678")
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private PropertyOwner recordEntity(Long id) {
        PropertyOwner record = PropertyOwner.builder()
                .name("Mao Leng")
                .email("owner@gmail.com")
                .phone("+85598765432")
                .build();
        record.setId(id);
        return record;
    }

    private AdminPropertyHouseRequest houseRequest() {
        AdminPropertyHouseRequest house = new AdminPropertyHouseRequest();
        house.setName("Two-Storey Villa");
        house.setAddress("Street 21, Phnom Penh");
        house.setCityProvince("Phnom Penh");
        house.setBedrooms(4);
        house.setBathrooms(3);
        return house;
    }

    private AdminPropertyPostRequest villaPost(PropertyOwnerRequest ownerRecord) {
        AdminPropertyPostRequest request = new AdminPropertyPostRequest();
        request.setPropertyType(PropertyType.VILLA);
        request.setOwnerRecord(ownerRecord);
        request.setHouse(houseRequest());
        request.setMonthlyRent(BigDecimal.valueOf(1500));
        return request;
    }

    private void stubReadBack(House house, PropertyOwner record) {
        when(houseRepository.findById(house.getId())).thenReturn(Optional.of(house));
        if (record != null) {
            when(propertyOwnerRepository.findById(record.getId()))
                    .thenReturn(Optional.of(record));
        }
        when(roomAmenityRepository.findByIdRoomId(anyLong())).thenReturn(List.of());
        when(roomUtilityRepository.findByRoomId(anyLong())).thenReturn(List.of());
        when(nearbyLocationRepository.findByHouseIdOrderByDistanceKmAscIdAsc(anyLong()))
                .thenReturn(List.of());
        when(houseImageRepository.findByHouseIdOrderBySortOrderAscIdAsc(anyLong()))
                .thenReturn(List.of());
    }

    // ---------- create with a non-login owner record ----------

    @Test
    void createVillaWithInlineOwnerRecordShouldNeverCreateUser() {
        allowSuperAdmin();
        AdminPropertyPostRequest request =
                villaPost(new PropertyOwnerRequest("Mao Leng", "owner@gmail.com",
                        "+85598765432", null, null));
        when(propertyOwnerRepository.save(any(PropertyOwner.class))).thenAnswer(invocation -> {
            PropertyOwner record = invocation.getArgument(0);
            record.setId(9L);
            return record;
        });
        when(houseRepository.save(any(House.class))).thenAnswer(invocation -> {
            House house = invocation.getArgument(0);
            house.setId(2L);
            return house;
        });
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> {
            Room room = invocation.getArgument(0);
            room.setId(1L);
            return room;
        });
        stubReadBack(houseEntity(2L, null, 9L), recordEntity(9L));

        AdminPropertyResponse response = adminPropertyService.create(request);

        assertThat(response.getRoomId()).isEqualTo(1L);
        assertThat(response.getPropertyOwnerId()).isEqualTo(9L);
        assertThat(response.isAccountOwner()).isFalse();
        assertThat(response.getOwnerName()).isEqualTo("Mao Leng");
        assertThat(response.getPublicationStatus()).isEqualTo(RoomPublicationStatus.DRAFT);

        // The whole point: no TeasJul account, no role assignment is ever created.
        verify(userRepository, never()).save(any(User.class));
        verify(userRoleRepository, never()).save(any());

        ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(roomCaptor.capture());
        assertThat(roomCaptor.getValue().getOwnerId()).isNull();
        assertThat(roomCaptor.getValue().getOwnerRecordId()).isEqualTo(9L);
        assertThat(roomCaptor.getValue().getPropertyType()).isEqualTo(PropertyType.VILLA);
        assertThat(roomCaptor.getValue().getPublicationStatus()).isEqualTo(RoomPublicationStatus.DRAFT);

        ArgumentCaptor<House> houseCaptor = ArgumentCaptor.forClass(House.class);
        verify(houseRepository).save(houseCaptor.capture());
        assertThat(houseCaptor.getValue().getOwnerRecordId()).isEqualTo(9L);
        // House exposes the record owner through the record link only.
        assertThat(houseCaptor.getValue().getName()).isEqualTo("Two-Storey Villa");
    }

    @Test
    void createVillaWithExistingOwnerRecordShouldNotResolveUserAccount() {
        allowSuperAdmin();
        AdminPropertyPostRequest request = villaPost(null);
        request.setOwnerRecordId(9L);
        PropertyOwner record = recordEntity(9L);
        when(propertyOwnerRepository.findById(9L)).thenReturn(Optional.of(record));
        when(houseRepository.save(any(House.class))).thenAnswer(invocation -> {
            House house = invocation.getArgument(0);
            house.setId(2L);
            return house;
        });
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> {
            Room room = invocation.getArgument(0);
            room.setId(1L);
            return room;
        });
        stubReadBack(houseEntity(2L, null, 9L), record);

        AdminPropertyResponse response = adminPropertyService.create(request);

        assertThat(response.getPropertyOwnerId()).isEqualTo(9L);
        assertThat(response.isAccountOwner()).isFalse();
        verify(userRepository, never()).findById(anyLong());
        verify(userRepository, never()).save(any(User.class));
        verify(userRoleRepository, never()).save(any());
    }

    // ---------- owner resolution ----------

    @Test
    void createShouldRejectWhenNoOwnerSpecified() {
        allowSuperAdmin();
        AdminPropertyPostRequest request = villaPost(null);
        request.setOwnerRecord(null);

        assertThatThrownBy(() -> adminPropertyService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Specify exactly one owner");

        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    void createShouldRejectWhenMultipleOwnersSpecified() {
        allowSuperAdmin();
        AdminPropertyPostRequest request = villaPost(null);
        request.setOwnerUserId(1L);
        request.setOwnerRecordId(9L);
        request.setOwnerRecord(new PropertyOwnerRequest("Mao Leng", null, null, null, null));

        assertThatThrownBy(() -> adminPropertyService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Specify exactly one owner");
    }

    @Test
    void createWithOwnerAccountShouldLinkOwnerIdAndResolveAccountOwner() {
        allowSuperAdmin();
        AdminPropertyPostRequest request = villaPost(null);
        request.setOwnerRecord(null);
        request.setOwnerUserId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(ownerEntity()));
        when(userRoleRepository.existsByUserIdAndRoleCode(1L, SecurityUtils.ROLE_OWNER))
                .thenReturn(true);
        when(houseRepository.save(any(House.class))).thenAnswer(invocation -> {
            House house = invocation.getArgument(0);
            house.setId(2L);
            return house;
        });
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> {
            Room room = invocation.getArgument(0);
            room.setId(1L);
            return room;
        });
        stubReadBack(houseEntity(2L, 1L, null), null);

        AdminPropertyResponse response = adminPropertyService.create(request);

        assertThat(response.getOwnerUserId()).isEqualTo(1L);
        assertThat(response.isAccountOwner()).isTrue();
        assertThat(response.getOwnerName()).isEqualTo("Mao Leng");

        ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(roomCaptor.capture());
        assertThat(roomCaptor.getValue().getOwnerId()).isEqualTo(1L);
        assertThat(roomCaptor.getValue().getOwnerRecordId()).isNull();
    }

    // ---------- ROOM post requires room type + number ----------

    @Test
    void createRoomWithoutRoomTypeShouldReject() {
        allowSuperAdmin();
        AdminPropertyPostRequest request = villaPost(null);
        request.setPropertyType(PropertyType.ROOM);
        request.setOwnerRecord(new PropertyOwnerRequest("Mao Leng", null, null, null, null));
        request.setRoomNumber("A-01");
        request.setRoomTypeId(null);
        request.setRoomType(null);

        assertThatThrownBy(() -> adminPropertyService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Room type is required for ROOM listings");

        verify(roomRepository, never()).save(any(Room.class));
    }

    // ---------- publish = self approval ----------

    @Test
    void createPublishedShouldSelfApproveByIssuingAdmin() {
        allowSuperAdmin();
        when(securityUtils.getCurrentUserId()).thenReturn(5L);
        AdminPropertyPostRequest request = villaPost(null);
        request.setOwnerRecord(new PropertyOwnerRequest("Mao Leng", null, null, null, null));
        request.setPublish(true);
        when(propertyOwnerRepository.save(any(PropertyOwner.class))).thenAnswer(invocation -> {
            PropertyOwner record = invocation.getArgument(0);
            record.setId(9L);
            return record;
        });
        when(houseRepository.save(any(House.class))).thenAnswer(invocation -> {
            House house = invocation.getArgument(0);
            house.setId(2L);
            return house;
        });
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> {
            Room room = invocation.getArgument(0);
            room.setId(1L);
            return room;
        });
        stubReadBack(houseEntity(2L, null, 9L), recordEntity(9L));

        AdminPropertyResponse response = adminPropertyService.create(request);

        assertThat(response.getPublicationStatus()).isEqualTo(RoomPublicationStatus.PUBLISHED);
        assertThat(response.getPublishedAt()).isNotNull();

        ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(roomCaptor.capture());
        assertThat(roomCaptor.getValue().getPublicationStatus())
                .isEqualTo(RoomPublicationStatus.PUBLISHED);
        assertThat(roomCaptor.getValue().getApprovedBy()).isEqualTo(5L);
        assertThat(roomCaptor.getValue().getApprovedAt()).isNotNull();
        assertThat(roomCaptor.getValue().getPublishedAt()).isNotNull();
    }

    // ---------- authorization ----------

    @Test
    void createShouldRejectNonSuperAdmin() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);
        AdminPropertyPostRequest request = villaPost(
                new PropertyOwnerRequest("Mao Leng", null, null, null, null));

        assertThatThrownBy(() -> adminPropertyService.create(request))
                .isInstanceOf(AccessDeniedException.class);

        verify(userRepository, never()).save(any(User.class));
        verify(roomRepository, never()).save(any(Room.class));
    }

    // ---------- owner records ----------

    @Test
    void listPropertyOwnersShouldReturnPagedRecords() {
        allowSuperAdmin();
        PropertyOwner record = recordEntity(5L);
        Page<PropertyOwner> page = new PageImpl<>(List.of(record), PageRequest.of(0, 10), 1);
        when(propertyOwnerRepository.findAll(any(Pageable.class))).thenReturn(page);

        PageResponse<PropertyOwnerResponse> result =
                adminPropertyService.listPropertyOwners(null, PageRequest.of(0, 10));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).id()).isEqualTo(5L);
        assertThat(result.content().get(0).name()).isEqualTo("Mao Leng");
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void createPropertyOwnerShouldPersistRecordWithoutUser() {
        allowSuperAdmin();
        when(propertyOwnerRepository.save(any(PropertyOwner.class))).thenAnswer(invocation -> {
            PropertyOwner record = invocation.getArgument(0);
            record.setId(8L);
            return record;
        });

        PropertyOwnerResponse response = adminPropertyService.createPropertyOwner(
                new PropertyOwnerRequest("Sokha", "sokha@gmail.com", "+855111", null, null));

        assertThat(response.id()).isEqualTo(8L);
        assertThat(response.name()).isEqualTo("Sokha");
        verify(userRepository, never()).save(any(User.class));
        verify(userRoleRepository, never()).save(any());
    }

    @Test
    void createPropertyOwnerShouldRejectBlankName() {
        allowSuperAdmin();

        assertThatThrownBy(() -> adminPropertyService.createPropertyOwner(
                new PropertyOwnerRequest(null, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Owner name is required");
    }

    @Test
    void deletePropertyOwnerShouldRemoveUnattachedRecord() {
        allowSuperAdmin();
        PropertyOwner record = recordEntity(5L);
        when(propertyOwnerRepository.findById(5L)).thenReturn(Optional.of(record));
        when(roomRepository.existsByOwnerRecordId(anyLong())).thenReturn(false);
        when(houseRepository.existsByOwnerRecordId(anyLong())).thenReturn(false);

        adminPropertyService.deletePropertyOwner(5L);

        verify(propertyOwnerRepository).delete(record);
    }

    @Test
    void deletePropertyOwnerShouldRejectAttachedRecord() {
        allowSuperAdmin();
        when(propertyOwnerRepository.findById(5L)).thenReturn(Optional.of(recordEntity(5L)));
        when(roomRepository.existsByOwnerRecordId(anyLong())).thenReturn(true);

        assertThatThrownBy(() -> adminPropertyService.deletePropertyOwner(5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be deleted");

        verify(propertyOwnerRepository, never()).delete(any(PropertyOwner.class));
    }

    @Test
    void deletePropertyOwnerShouldThrow404WhenMissing() {
        allowSuperAdmin();
        when(propertyOwnerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminPropertyService.deletePropertyOwner(99L))
                .isInstanceOf(PropertyOwnerNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void deletePropertyOwnerShouldRejectNonSuperAdmin() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> adminPropertyService.deletePropertyOwner(5L))
                .isInstanceOf(AccessDeniedException.class);

        verify(propertyOwnerRepository, never()).delete(any(PropertyOwner.class));
    }

    private House houseEntity(Long id, Long ownerId, Long ownerRecordId) {
        House house = new House();
        house.setId(id);
        if (ownerId != null) {
            house.setOwner(ownerEntity());
        }
        house.setOwnerRecordId(ownerRecordId);
        house.setName("Two-Storey Villa");
        house.setAddress("Street 21, Phnom Penh");
        return house;
    }
}