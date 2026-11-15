package com.example.spring_boot_project_api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.example.spring_boot_project_api.dto.request.PublicRoomQueryRequest;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.PublicRoomResponse;
import com.example.spring_boot_project_api.dto.response.PublicRoomTypeResponse;
import com.example.spring_boot_project_api.enums.UtilityBillingMethod;
import com.example.spring_boot_project_api.enums.UtilityRateStatus;
import com.example.spring_boot_project_api.enums.UtilityTypeStatus;
import com.example.spring_boot_project_api.enums.Furnishing;
import com.example.spring_boot_project_api.enums.ListingScope;
import com.example.spring_boot_project_api.enums.NearbyLocationCategory;
import com.example.spring_boot_project_api.enums.PropertyType;
import com.example.spring_boot_project_api.enums.PublicRoomSort;
import com.example.spring_boot_project_api.enums.RoomPublicationStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.NearbyLocation;
import com.example.spring_boot_project_api.model.PropertyOwner;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.RoomType;
import com.example.spring_boot_project_api.model.RoomUtility;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UtilityRate;
import com.example.spring_boot_project_api.model.UtilityType;
import com.example.spring_boot_project_api.repository.HouseImageRepository;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.NearbyLocationRepository;
import com.example.spring_boot_project_api.repository.PropertyOwnerRepository;
import com.example.spring_boot_project_api.repository.PublicRoomGroupProjection;
import com.example.spring_boot_project_api.repository.RoomAmenityRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.RoomTypeImageRepository;
import com.example.spring_boot_project_api.repository.RoomTypeRepository;
import com.example.spring_boot_project_api.repository.RoomUtilityRepository;
import com.example.spring_boot_project_api.repository.UtilityRateRepository;
import com.example.spring_boot_project_api.repository.UtilityTypeRepository;

class PublicRoomServiceImplTest {

    private RoomRepository roomRepository;
    private HouseRepository houseRepository;
    private RoomTypeRepository roomTypeRepository;
    private NearbyLocationRepository nearbyLocationRepository;
    private RoomUtilityRepository roomUtilityRepository;
    private UtilityTypeRepository utilityTypeRepository;
    private UtilityRateRepository utilityRateRepository;
    private PropertyOwnerRepository propertyOwnerRepository;
    private PublicRoomServiceImpl service;

    @BeforeEach
    void setUp() {
        roomRepository = mock(RoomRepository.class);
        houseRepository = mock(HouseRepository.class);
        roomTypeRepository = mock(RoomTypeRepository.class);
        nearbyLocationRepository = mock(NearbyLocationRepository.class);
        roomUtilityRepository = mock(RoomUtilityRepository.class);
        utilityTypeRepository = mock(UtilityTypeRepository.class);
        utilityRateRepository = mock(UtilityRateRepository.class);
        propertyOwnerRepository = mock(PropertyOwnerRepository.class);
        service = new PublicRoomServiceImpl(
                roomRepository,
                houseRepository,
                roomTypeRepository,
                mock(HouseImageRepository.class),
                mock(RoomTypeImageRepository.class),
                mock(RoomAmenityRepository.class),
                nearbyLocationRepository,
                roomUtilityRepository,
                utilityTypeRepository,
                utilityRateRepository,
                propertyOwnerRepository);
    }

    private Room publishedRoom() {
        Room room = new Room();
        room.setId(1L);
        room.setName("Sunset Room");
        room.setRoomNumber("101");
        room.setHouseId(7L);
        room.setRoomTypeId(3L);
        room.setStatus(RoomStatus.AVAILABLE);
        room.setPublicationStatus(RoomPublicationStatus.PUBLISHED);
        room.setPublishedAt(LocalDateTime.of(2026, 9, 1, 12, 0));
        room.setMonthlyRent(new BigDecimal("350.00"));
        room.setPropertyType(PropertyType.ROOM);
        room.setListingScope(ListingScope.INDIVIDUAL_ROOM);
        room.setFurnishing(Furnishing.FULLY_FURNISHED);
        room.setAvailableFrom(LocalDate.of(2026, 10, 1));
        room.setAdvancePayment(new BigDecimal("150.00"));
        room.setBedrooms(1);
        room.setBathrooms(1);
        room.setAreaSqm(new BigDecimal("20.00"));
        room.setLandSizeSqm(new BigDecimal("10.00"));
        room.setNumberOfFloors(1);
        return room;
    }

    private House house() {
        User owner = new User();
        owner.setId(42L);
        owner.setFirstName("Lanh");
        owner.setLastName("Phalla");
        owner.setPhone("+855 12 345 678");
        owner.setEmail("lanh.phalla@example.com");
        return House.builder()
                .id(7L)
                .owner(owner)
                .name("Villa Mondul")
                .address("Street 200")
                .postalCode("12000")
                .cityProvince("Phnom Penh")
                .latitude(new BigDecimal("11.5564"))
                .longitude(new BigDecimal("104.9282"))
                .googleMapsUrl("https://www.google.com/maps/place/Phnom+Penh")
                .build();
    }

    private PublicRoomGroupProjection group(Long roomId, Long houseId, Long roomTypeId,
            Long availableRoomCount, BigDecimal fromRent) {
        return new PublicRoomGroupProjection() {
            @Override public Long getRepresentativeRoomId() { return roomId; }
            @Override public Long getHouseId() { return houseId; }
            @Override public Long getRoomTypeId() { return roomTypeId; }
            @Override public Long getAvailableRoomCount() { return availableRoomCount; }
            @Override public BigDecimal getFromRent() { return fromRent; }
        };
    }

    @Test
    void listRoomsExposesListingAndHouseLocationFields() {
        Room room = publishedRoom();
        when(roomRepository.findPublicRoomGroups(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(Boolean.class), any(), any(Boolean.class), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(group(1L, 7L, 3L, 1L,
                        new BigDecimal("350.00")))));
        when(roomRepository.findAllById(List.of(1L))).thenReturn(List.of(room));
        when(houseRepository.findAllById(List.of(7L))).thenReturn(List.of(house()));
        when(roomTypeRepository.findAllById(List.of(3L))).thenReturn(List.of());
        when(nearbyLocationRepository.findByHouseIdIn(List.of(7L)))
                .thenReturn(List.of(nearbyLocation(100L)));

        PageResponse<PublicRoomResponse> result =
                service.listRooms(null, null, null, PageRequest.of(0, 10));

        PublicRoomResponse item = result.content().get(0);
        assertThat(item.propertyType()).isEqualTo(PropertyType.ROOM);
        assertThat(item.listingScope()).isEqualTo(ListingScope.INDIVIDUAL_ROOM);
        assertThat(item.furnishing()).isEqualTo(Furnishing.FULLY_FURNISHED);
        assertThat(item.availableFrom()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(item.advancePayment()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(item.bedrooms()).isEqualTo(1);
        assertThat(item.bathrooms()).isEqualTo(1);
        assertThat(item.areaSqm()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(item.landSizeSqm()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(item.numberOfFloors()).isEqualTo(1);
        assertThat(item.cityProvince()).isEqualTo("Phnom Penh");
        assertThat(item.latitude()).isEqualByComparingTo(new BigDecimal("11.5564"));
        assertThat(item.longitude()).isEqualByComparingTo(new BigDecimal("104.9282"));
        assertThat(item.googleMapsUrl()).isEqualTo("https://www.google.com/maps/place/Phnom+Penh");
        assertThat(item.nearbyLocations()).hasSize(1);
        assertThat(item.nearbyLocations().get(0).id()).isEqualTo(100L);
        assertThat(item.nearbyLocations().get(0).name()).isEqualTo("Royal University");
        assertThat(item.nearbyLocations().get(0).category()).isEqualTo(NearbyLocationCategory.UNIVERSITY);
        assertThat(item.nearbyLocations().get(0).distanceKm()).isEqualByComparingTo(new BigDecimal("1.50"));
        assertThat(item.nearbyLocations().get(0).travelMinutes()).isEqualTo(12);
        assertThat(item.availableRoomCount()).isEqualTo(1L);
        assertThat(item.fromRent()).isEqualByComparingTo("350.00");
    }

    private NearbyLocation nearbyLocation(Long id) {
        return NearbyLocation.builder()
                .id(id)
                .house(house())
                .name("Royal University")
                .category(NearbyLocationCategory.UNIVERSITY)
                .distanceKm(new BigDecimal("1.50"))
                .travelMinutes(12)
                .build();
    }

    @Test
    void listRoomsOnlyQueriesPublishedAndAvailableRooms() {
        when(roomRepository.findPublicRoomGroups(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(Boolean.class), any(), any(Boolean.class), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        service.listRooms(null, null, null, PageRequest.of(0, 10));

        ArgumentCaptor<RoomPublicationStatus> publication = ArgumentCaptor.forClass(RoomPublicationStatus.class);
        ArgumentCaptor<RoomStatus> status = ArgumentCaptor.forClass(RoomStatus.class);
        ArgumentCaptor<String> search = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<BigDecimal> minRent = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<BigDecimal> maxRent = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);

        verify(roomRepository).findPublicRoomGroups(
                publication.capture(), status.capture(), search.capture(),
                any(), any(), any(), any(), any(), minRent.capture(), maxRent.capture(),
                any(), any(), any(), any(), any(), any(), any(Boolean.class), any(), any(Boolean.class), any(), any(), pageable.capture());

        assertThat(publication.getValue()).isEqualTo(RoomPublicationStatus.PUBLISHED);
        assertThat(status.getValue()).isEqualTo(RoomStatus.AVAILABLE);
    }

    @Test
    void getRoomDetailExposesListingAndHouseLocationFields() {
        Room room = publishedRoom();
        when(roomRepository.findByIdAndPublicationStatusAndStatus(1L,
                RoomPublicationStatus.PUBLISHED, RoomStatus.AVAILABLE))
                .thenReturn(Optional.of(room));
        when(houseRepository.findById(7L)).thenReturn(Optional.of(house()));
        when(roomTypeRepository.findById(3L)).thenReturn(Optional.empty());
        when(nearbyLocationRepository.findByHouseIdOrderByDistanceKmAscIdAsc(7L))
                .thenReturn(List.of(nearbyLocation(100L)));
        RoomUtility included = RoomUtility.builder().id(10L).roomId(1L).utilityTypeId(20L)
                .status(UtilityTypeStatus.ACTIVE).isIncluded(true).build();
        when(roomUtilityRepository.findByRoomIdInAndStatus(List.of(1L), UtilityTypeStatus.ACTIVE))
                .thenReturn(List.of(included));
        when(utilityTypeRepository.findByIdInAndStatus(List.of(20L), UtilityTypeStatus.ACTIVE))
                .thenReturn(List.of(UtilityType.builder().id(20L).name("Internet").unit("month")
                        .billingMethod(UtilityBillingMethod.FIXED)
                        .status(UtilityTypeStatus.ACTIVE).build()));

        PublicRoomResponse detail = service.getRoom(1L);

        assertThat(detail.propertyType()).isEqualTo(PropertyType.ROOM);
        assertThat(detail.listingScope()).isEqualTo(ListingScope.INDIVIDUAL_ROOM);
        assertThat(detail.furnishing()).isEqualTo(Furnishing.FULLY_FURNISHED);
        assertThat(detail.availableFrom()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(detail.advancePayment()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(detail.bedrooms()).isEqualTo(1);
        assertThat(detail.bathrooms()).isEqualTo(1);
        assertThat(detail.areaSqm()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(detail.landSizeSqm()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(detail.numberOfFloors()).isEqualTo(1);
        assertThat(detail.cityProvince()).isEqualTo("Phnom Penh");
        assertThat(detail.latitude()).isEqualByComparingTo(new BigDecimal("11.5564"));
        assertThat(detail.longitude()).isEqualByComparingTo(new BigDecimal("104.9282"));
        assertThat(detail.googleMapsUrl()).isEqualTo("https://www.google.com/maps/place/Phnom+Penh");
        assertThat(detail.nearbyLocations()).hasSize(1);
        assertThat(detail.nearbyLocations().get(0).name()).isEqualTo("Royal University");
        assertThat(detail.nearbyLocations().get(0).category()).isEqualTo(NearbyLocationCategory.UNIVERSITY);
        assertThat(detail.utilities()).singleElement().satisfies(utility -> {
            assertThat(utility.name()).isEqualTo("Internet");
            assertThat(utility.isIncluded()).isTrue();
            assertThat(utility.rate()).isNull();
        });
        assertThat(detail.owner()).satisfies(owner -> {
            assertThat(owner.id()).isEqualTo(42L);
            assertThat(owner.name()).isEqualTo("Lanh Phalla");
            assertThat(owner.phone()).isEqualTo("+855 12 345 678");
            assertThat(owner.email()).isEqualTo("lanh.phalla@example.com");
        });
    }

    @Test
    void getRoomDetailExposesRecordOwnerWhenHouseHasNoLoginOwner() {
        Room room = publishedRoom();
        room.setOwnerId(null);
        PropertyOwner ownerRecord = PropertyOwner.builder()
                .id(77L)
                .name("Sok Dara")
                .phone("+855 98 765 432")
                .email("sok.dara@gmail.com")
                .build();
        House recordHouse = House.builder()
                .id(7L)
                .ownerRecordId(77L)
                .name("Riverside Villa")
                .address("Street 200")
                .build();
        when(roomRepository.findByIdAndPublicationStatusAndStatus(1L,
                RoomPublicationStatus.PUBLISHED, RoomStatus.AVAILABLE))
                .thenReturn(Optional.of(room));
        when(houseRepository.findById(7L)).thenReturn(Optional.of(recordHouse));
        when(propertyOwnerRepository.findById(77L)).thenReturn(Optional.of(ownerRecord));
        when(roomTypeRepository.findById(3L)).thenReturn(Optional.empty());
        when(nearbyLocationRepository.findByHouseIdOrderByDistanceKmAscIdAsc(7L))
                .thenReturn(List.of());
        when(roomUtilityRepository.findByRoomIdInAndStatus(List.of(1L), UtilityTypeStatus.ACTIVE))
                .thenReturn(List.of());

        PublicRoomResponse detail = service.getRoom(1L);

        assertThat(detail.owner()).satisfies(owner -> {
            assertThat(owner.id()).isEqualTo(77L);
            assertThat(owner.name()).isEqualTo("Sok Dara");
            assertThat(owner.phone()).isEqualTo("+855 98 765 432");
            assertThat(owner.email()).isEqualTo("sok.dara@gmail.com");
        });
    }

    @Test
    void getRoomDetailIncludesAllPublishedAvailableRoomsInItsHouseAndRoomTypeGroup() {
        Room representative = publishedRoom();
        Room second = publishedRoom();
        second.setId(2L);
        second.setRoomNumber("102");
        second.setFloorNumber(2);
        second.setMonthlyRent(new BigDecimal("375.00"));
        second.setSecurityDeposit(new BigDecimal("75.00"));
        Room unavailable = publishedRoom();
        unavailable.setId(3L);
        unavailable.setRoomNumber("103");
        unavailable.setStatus(RoomStatus.OCCUPIED);

        when(roomRepository.findByIdAndPublicationStatusAndStatus(1L,
                RoomPublicationStatus.PUBLISHED, RoomStatus.AVAILABLE))
                .thenReturn(Optional.of(representative));
        when(roomRepository
                .findByHouseIdAndRoomTypeIdAndPublicationStatusAndStatusOrderByFloorNumberAscRoomNumberAscIdAsc(
                        7L, 3L, RoomPublicationStatus.PUBLISHED, RoomStatus.AVAILABLE))
                .thenReturn(List.of(representative, second));

        PublicRoomResponse detail = service.getRoom(1L);

        assertThat(detail.availableRooms()).extracting(room -> room.roomNumber())
                .containsExactly("101", "102");
        assertThat(detail.availableRooms().get(1).floorNumber()).isEqualTo(2);
        assertThat(detail.availableRooms().get(1).monthlyRent()).isEqualByComparingTo("375.00");
        assertThat(detail.availableRooms().get(1).securityDeposit()).isEqualByComparingTo("75.00");
        assertThat(detail.availableRooms()).extracting(room -> room.id()).doesNotContain(unavailable.getId());
        verify(roomRepository)
                .findByHouseIdAndRoomTypeIdAndPublicationStatusAndStatusOrderByFloorNumberAscRoomNumberAscIdAsc(
                        7L, 3L, RoomPublicationStatus.PUBLISHED, RoomStatus.AVAILABLE);
    }

    @Test
    void getRoomDetailOnlyFindsPublishedRooms() {
        when(roomRepository.findByIdAndPublicationStatusAndStatus(eq(99L),
                eq(RoomPublicationStatus.PUBLISHED), eq(RoomStatus.AVAILABLE)))
                .thenReturn(Optional.empty());

        try {
            service.getRoom(99L);
        } catch (Exception expected) {
            assertThat(expected).isInstanceOf(RuntimeException.class);
        }

        verify(roomRepository).findByIdAndPublicationStatusAndStatus(99L,
                RoomPublicationStatus.PUBLISHED, RoomStatus.AVAILABLE);
    }

    @Test
    void publicUtilitiesUseCurrentEffectiveRateAndNeverExposeAIncludedRate() {
        Room room = publishedRoom();
        when(roomRepository.findPublicRoomGroups(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(Boolean.class), any(), any(Boolean.class), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(group(1L, 7L, 3L, 1L,
                        new BigDecimal("350.00")))));
        when(roomRepository.findAllById(List.of(1L))).thenReturn(List.of(room));
        when(houseRepository.findAllById(List.of(7L))).thenReturn(List.of(house()));
        when(roomTypeRepository.findAllById(List.of(3L))).thenReturn(List.of());

        RoomUtility included = RoomUtility.builder().id(10L).roomId(1L).utilityTypeId(20L)
                .status(UtilityTypeStatus.ACTIVE).isIncluded(true).build();
        RoomUtility billed = RoomUtility.builder().id(11L).roomId(1L).utilityTypeId(21L)
                .status(UtilityTypeStatus.ACTIVE).isIncluded(false).build();
        when(roomUtilityRepository.findByRoomIdInAndStatus(List.of(1L), UtilityTypeStatus.ACTIVE))
                .thenReturn(List.of(included, billed));

        UtilityType internet = UtilityType.builder().id(20L).name("Internet").unit("month")
                .billingMethod(UtilityBillingMethod.FIXED).status(UtilityTypeStatus.ACTIVE).build();
        UtilityType water = UtilityType.builder().id(21L).name("Water").unit("m3")
                .billingMethod(UtilityBillingMethod.METERED).status(UtilityTypeStatus.ACTIVE).build();
        when(utilityTypeRepository.findByIdInAndStatus(List.of(20L, 21L), UtilityTypeStatus.ACTIVE))
                .thenReturn(List.of(internet, water));
        UtilityRate waterRate = UtilityRate.builder().id(30L).houseId(7L).utilityTypeId(21L)
                .rate(new BigDecimal("1.2500")).effectiveFrom(LocalDate.now().minusDays(1))
                .status(UtilityRateStatus.ACTIVE).build();
        when(utilityRateRepository.findCurrentEffectiveRates(List.of(7L), List.of(21L),
                LocalDate.now(), UtilityRateStatus.ACTIVE)).thenReturn(List.of(waterRate));

        PublicRoomResponse response = service.listRooms(null, null, null, PageRequest.of(0, 10))
                .content().get(0);

        assertThat(response.utilities()).hasSize(2);
        assertThat(response.utilities().get(0).id()).isEqualTo(20L);
        assertThat(response.utilities().get(0).rate()).isNull();
        assertThat(response.utilities().get(0).isIncluded()).isTrue();
        assertThat(response.utilities().get(1).rate()).isEqualByComparingTo("1.2500");
        assertThat(response.utilities().get(1).isIncluded()).isFalse();
    }

    @Test
    void publicUtilitiesAreFetchedInBulkForTheListingPage() {
        Room first = publishedRoom();
        Room second = publishedRoom();
        second.setId(2L);
        second.setRoomNumber("102");
        second.setRoomTypeId(4L);
        when(roomRepository.findPublicRoomGroups(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(Boolean.class), any(), any(Boolean.class), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(
                        group(1L, 7L, 3L, 2L, new BigDecimal("350.00")),
                        group(2L, 7L, 4L, 2L, new BigDecimal("350.00")))));
        when(roomRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(first, second));
        when(houseRepository.findAllById(List.of(7L))).thenReturn(List.of(house()));
        when(roomTypeRepository.findAllById(List.of(3L))).thenReturn(List.of());
        RoomUtility firstAssignment = RoomUtility.builder().roomId(1L).utilityTypeId(20L)
                .status(UtilityTypeStatus.ACTIVE).build();
        RoomUtility secondAssignment = RoomUtility.builder().roomId(2L).utilityTypeId(20L)
                .status(UtilityTypeStatus.ACTIVE).build();
        when(roomUtilityRepository.findByRoomIdInAndStatus(List.of(1L, 2L), UtilityTypeStatus.ACTIVE))
                .thenReturn(List.of(firstAssignment, secondAssignment));
        when(utilityTypeRepository.findByIdInAndStatus(List.of(20L), UtilityTypeStatus.ACTIVE))
                .thenReturn(List.of(UtilityType.builder().id(20L).name("Water").unit("m3")
                        .billingMethod(UtilityBillingMethod.METERED)
                        .status(UtilityTypeStatus.ACTIVE).build()));
        when(utilityRateRepository.findCurrentEffectiveRates(List.of(7L), List.of(20L),
                LocalDate.now(), UtilityRateStatus.ACTIVE)).thenReturn(List.of());

        service.listRooms(null, null, null, PageRequest.of(0, 10));

        verify(roomUtilityRepository).findByRoomIdInAndStatus(List.of(1L, 2L),
                UtilityTypeStatus.ACTIVE);
        verify(utilityTypeRepository).findByIdInAndStatus(List.of(20L), UtilityTypeStatus.ACTIVE);
        verify(utilityRateRepository).findCurrentEffectiveRates(List.of(7L), List.of(20L),
                LocalDate.now(), UtilityRateStatus.ACTIVE);
    }

    @Test
    void listGroupsVisibleRoomsByHouseAndRoomTypeWithMinimumRent() {
        Room sunriseModern = publishedRoom();
        sunriseModern.setMonthlyRent(new BigDecimal("175.00"));
        Room riversideModern = publishedRoom();
        riversideModern.setId(2L);
        riversideModern.setHouseId(8L);
        riversideModern.setRoomNumber("B-1");
        riversideModern.setMonthlyRent(new BigDecimal("180.00"));
        when(roomRepository.findPublicRoomGroups(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(Boolean.class), any(), any(Boolean.class), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(
                        group(1L, 7L, 3L, 3L, new BigDecimal("150.00")),
                        group(2L, 8L, 3L, 4L, new BigDecimal("160.00")))));
        when(roomRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(List.of(sunriseModern, riversideModern));
        when(houseRepository.findAllById(List.of(7L, 8L)))
                .thenReturn(List.of(house(), House.builder().id(8L).name("Riverside").build()));
        when(roomTypeRepository.findAllById(List.of(3L))).thenReturn(List.of());

        PageResponse<PublicRoomResponse> result =
                service.listRooms(null, null, null, PageRequest.of(0, 10));

        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0).availableRoomCount()).isEqualTo(3L);
        assertThat(result.content().get(0).fromRent()).isEqualByComparingTo("150.00");
        assertThat(result.content().get(1).availableRoomCount()).isEqualTo(4L);
        assertThat(result.content().get(1).fromRent()).isEqualByComparingTo("160.00");
        assertThat(result.content()).extracting(PublicRoomResponse::houseId)
                .containsExactly(7L, 8L);
        verify(roomRepository).findPublicRoomGroups(eq(RoomPublicationStatus.PUBLISHED),
                eq(RoomStatus.AVAILABLE), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(Boolean.class), any(), any(Boolean.class), any(), any(), eq(PageRequest.of(0, 10)));
    }

    @Test
    void listRoomsForwardsRoomTypeAndSortToRepository() {
        when(roomRepository.findPublicRoomGroups(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(Boolean.class), any(), any(Boolean.class), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        service.listRooms(new PublicRoomQueryRequest("room", "Phnom Penh", PropertyType.ROOM, 5L,
                null, null, null, null, null, null, null, null, null, null, null, null,
                PublicRoomSort.PRICE_ASC), PageRequest.of(0, 10));

        ArgumentCaptor<Long> roomTypeId = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> sortOrder = ArgumentCaptor.forClass(String.class);
        verify(roomRepository).findPublicRoomGroups(eq(RoomPublicationStatus.PUBLISHED),
                eq(RoomStatus.AVAILABLE), eq("room"), eq("Phnom Penh"), eq(PropertyType.ROOM),
                roomTypeId.capture(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(Boolean.class), any(), any(Boolean.class), any(),
                sortOrder.capture(), any());
        assertThat(roomTypeId.getValue()).isEqualTo(5L);
        assertThat(sortOrder.getValue()).isEqualTo("PRICE_ASC");
    }

    @Test
    void getPublicRoomTypesReturnsOnlyPublishedRoomTypesSortedByName() {
        when(roomRepository.findDistinctPublicRoomTypeIds(RoomPublicationStatus.PUBLISHED,
                RoomStatus.AVAILABLE)).thenReturn(List.of(3L, 5L));
        RoomType zulu = new RoomType();
        zulu.setId(5L);
        zulu.setName("Zulu");
        RoomType alpha = new RoomType();
        alpha.setId(3L);
        alpha.setName("Alpha");
        when(roomTypeRepository.findAllById(List.of(3L, 5L))).thenReturn(List.of(zulu, alpha));

        List<PublicRoomTypeResponse> result = service.getPublicRoomTypes();

        assertThat(result).extracting(PublicRoomTypeResponse::id)
                .containsExactly(3L, 5L);
        assertThat(result).extracting(PublicRoomTypeResponse::name)
                .containsExactly("Alpha", "Zulu");
    }

    @Test
    void getPublicRoomTypesReturnsEmptyWhenNoPublishedRoomTypes() {
        when(roomRepository.findDistinctPublicRoomTypeIds(RoomPublicationStatus.PUBLISHED,
                RoomStatus.AVAILABLE)).thenReturn(List.of());
        assertThat(service.getPublicRoomTypes()).isEmpty();
    }
}
