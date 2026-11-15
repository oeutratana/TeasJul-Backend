package com.example.spring_boot_project_api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.mockito.ArgumentCaptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.example.spring_boot_project_api.dto.request.CreateHouseRequest;
import com.example.spring_boot_project_api.dto.request.UpdateHouseRequest;
import com.example.spring_boot_project_api.dto.response.HouseResponse;
import com.example.spring_boot_project_api.mapper.HouseMapper;
import com.example.spring_boot_project_api.mapper.RentalContractMapper;
import com.example.spring_boot_project_api.mapper.RoomMapper;
import com.example.spring_boot_project_api.mapper.TenantMapper;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.ContractTenantRepository;
import com.example.spring_boot_project_api.repository.HouseImageRepository;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.HouseRoomCountProjection;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HouseServiceImplTest {

    private static final long OWNER_ID = 7L;

    private HouseRepository houseRepository;
    private RoomRepository roomRepository;
    private SecurityUtils securityUtils;
    private HouseMapper houseMapper;
    private HouseServiceImpl houseService;

    @BeforeEach
    void setUp() {
        houseRepository = mock(HouseRepository.class);
        roomRepository = mock(RoomRepository.class);
        securityUtils = mock(SecurityUtils.class);
        houseMapper = new HouseMapper();

        houseService = new HouseServiceImpl(
                houseRepository,
                mock(HouseImageRepository.class),
                roomRepository,
                mock(RentalContractRepository.class),
                mock(ContractTenantRepository.class),
                mock(TenantRepository.class),
                mock(UserHouseAccessRepository.class),
                houseMapper,
                mock(RoomMapper.class),
                mock(TenantMapper.class),
                mock(RentalContractMapper.class),
                securityUtils);

        when(securityUtils.getCurrentUserId()).thenReturn(OWNER_ID);
        when(securityUtils.getCurrentUser()).thenReturn(owner());
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(true);
        when(houseRepository.save(any(House.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private User owner() {
        return User.builder().id(OWNER_ID).username("owner1").build();
    }

    @Test
    void createHousePreservesCityProvinceAndGoogleMapsUrl() {
        CreateHouseRequest request = new CreateHouseRequest(
                "Villa Mondul", "villa-mondul", "H001", "Street 200", "12000",
                new BigDecimal("11.5564"), new BigDecimal("104.9282"),
                "Phnom Penh", "https://www.google.com/maps/place/Phnom+Penh",
                5, 3, "A nice villa", null, null, null, null, null, null, null, null, null);

        HouseResponse response = houseService.createHouse(request);

        assertThat(response.cityProvince()).isEqualTo("Phnom Penh");
        assertThat(response.googleMapsUrl()).isEqualTo("https://www.google.com/maps/place/Phnom+Penh");
    }

    @Test
    void updateHousePreservesCityProvinceAndGoogleMapsUrl() {
        House house = House.builder().id(1L).owner(owner()).name("Villa Mondul").build();
        when(houseRepository.findById(1L)).thenReturn(Optional.of(house));

        UpdateHouseRequest request = new UpdateHouseRequest(
                null, null, null, null, null, null, null,
                "Siem Reap", "https://www.google.com/maps/place/Siem+Reap",
                null, null, null, null, null, null, null, null, null, null, null, null, null);

        HouseResponse response = houseService.updateHouse(1L, request);

        assertThat(response.cityProvince()).isEqualTo("Siem Reap");
        assertThat(response.googleMapsUrl()).isEqualTo("https://www.google.com/maps/place/Siem+Reap");
    }

    @Test
    void getHouseByIdReturnsCityProvinceAndGoogleMapsUrl() {
        House house = House.builder()
                .id(1L)
                .owner(owner())
                .name("Villa Mondul")
                .cityProvince("Kampot")
                .googleMapsUrl("https://www.google.com/maps/place/Kampot")
                .build();
        when(houseRepository.findById(1L)).thenReturn(Optional.of(house));
        when(houseRepository.findById(anyLong())).thenReturn(Optional.of(house));

        HouseResponse response = houseService.getHouseById(1L);

        assertThat(response.cityProvince()).isEqualTo("Kampot");
        assertThat(response.googleMapsUrl()).isEqualTo("https://www.google.com/maps/place/Kampot");
    }

    // ------------------------------------------------------------------
    // House form UX: location fields and derived room counts
    // ------------------------------------------------------------------

    private HouseRoomCountProjection roomCount(Long houseId, Long count) {
        return new HouseRoomCountProjection() {
            @Override
            public Long getHouseId() {
                return houseId;
            }

            @Override
            public Long getRoomCount() {
                return count;
            }
        };
    }

    private CreateHouseRequest createRequest(Integer totalRooms, Integer availableRooms) {
        return new CreateHouseRequest(
                "Lanh Phallla Residence", "lanh-phallla-residence", "H002", "Street 200", "12000",
                null, null, "Phnom Penh", "https://maps.google.com/?q=11.5564,104.9282",
                totalRooms, availableRooms, "A nice residence",
                null, null, null, null, null, null, null, null, null);
    }

    @Test
    void createHouseSucceedsWithoutLatitudeLongitudeOrRoomCountInputs() {
        HouseResponse response = houseService.createHouse(createRequest(null, null));

        assertThat(response.latitude()).isNull();
        assertThat(response.longitude()).isNull();
        assertThat(response.totalRooms()).isZero();
        assertThat(response.availableRooms()).isZero();
        assertThat(response.googleMapsUrl())
                .isEqualTo("https://maps.google.com/?q=11.5564,104.9282");

        ArgumentCaptor<House> captor = ArgumentCaptor.forClass(House.class);
        verify(houseRepository).save(captor.capture());
        assertThat(captor.getValue().getTotalRooms()).isZero();
        assertThat(captor.getValue().getAvailableRooms()).isZero();
    }

    @Test
    void createHouseIgnoresClientSuppliedRoomCounts() {
        HouseResponse response = houseService.createHouse(createRequest(999, 999));

        assertThat(response.totalRooms()).isZero();
        assertThat(response.availableRooms()).isZero();

        ArgumentCaptor<House> captor = ArgumentCaptor.forClass(House.class);
        verify(houseRepository).save(captor.capture());
        assertThat(captor.getValue().getTotalRooms()).isZero();
        assertThat(captor.getValue().getAvailableRooms()).isZero();
    }

    @Test
    void getHouseByIdDerivesRoomCountsFromRoomRecords() {
        House house = House.builder().id(1L).owner(owner()).name("Villa Mondul").build();
        when(houseRepository.findById(1L)).thenReturn(Optional.of(house));
        when(roomRepository.countByHouseId(1L)).thenReturn(12L);
        when(roomRepository.countByHouseIdAndStatus(1L, RoomStatus.AVAILABLE)).thenReturn(5L);

        HouseResponse response = houseService.getHouseById(1L);

        assertThat(response.totalRooms()).isEqualTo(12);
        assertThat(response.availableRooms()).isEqualTo(5);
    }

    @Test
    void getHouseByIdAvailableCountReflectsRoomStatusChanges() {
        House house = House.builder().id(1L).owner(owner()).name("Villa Mondul").build();
        when(houseRepository.findById(1L)).thenReturn(Optional.of(house));
        when(roomRepository.countByHouseId(1L)).thenReturn(10L);
        when(roomRepository.countByHouseIdAndStatus(1L, RoomStatus.AVAILABLE)).thenReturn(7L);

        assertThat(houseService.getHouseById(1L).availableRooms()).isEqualTo(7);

        // A room moves AVAILABLE -> OCCUPIED, so the derived count drops.
        when(roomRepository.countByHouseIdAndStatus(1L, RoomStatus.AVAILABLE)).thenReturn(6L);

        assertThat(houseService.getHouseById(1L).availableRooms()).isEqualTo(6);
    }

    @Test
    void updateHouseIgnoresClientSuppliedRoomCountsAndDerivesThem() {
        House house = House.builder().id(1L).owner(owner()).name("Villa Mondul").build();
        when(houseRepository.findById(1L)).thenReturn(Optional.of(house));
        when(roomRepository.countByHouseId(1L)).thenReturn(4L);
        when(roomRepository.countByHouseIdAndStatus(1L, RoomStatus.AVAILABLE)).thenReturn(3L);

        UpdateHouseRequest request = new UpdateHouseRequest(
                null, null, null, null, null, null, null, null, null,
                999, 999, null, null, null, null, null, null, null, null, null, null, null);

        HouseResponse response = houseService.updateHouse(1L, request);

        assertThat(response.totalRooms()).isEqualTo(4);
        assertThat(response.availableRooms()).isEqualTo(3);
        assertThat(house.getTotalRooms()).isNotEqualTo(999);
        assertThat(house.getAvailableRooms()).isNotEqualTo(999);
    }

    @Test
    void getMyHousesDerivesRoomCountsPerHouse() {
        House h1 = House.builder().id(1L).owner(owner()).name("A").build();
        House h2 = House.builder().id(2L).owner(owner()).name("B").build();
        when(houseRepository.findAllByOwnerIdOrderByCreatedAtDesc(OWNER_ID)).thenReturn(List.of(h1, h2));
        when(roomRepository.countRoomsByHouseIds(any()))
                .thenReturn(List.of(roomCount(1L, 10L), roomCount(2L, 6L)));
        when(roomRepository.countRoomsByHouseIdsAndStatus(any(), eq(RoomStatus.AVAILABLE)))
                .thenReturn(List.of(roomCount(1L, 4L), roomCount(2L, 0L)));

        List<HouseResponse> responses = houseService.getMyHouses();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).totalRooms()).isEqualTo(10);
        assertThat(responses.get(0).availableRooms()).isEqualTo(4);
        assertThat(responses.get(1).totalRooms()).isEqualTo(6);
        assertThat(responses.get(1).availableRooms()).isZero();
    }
}