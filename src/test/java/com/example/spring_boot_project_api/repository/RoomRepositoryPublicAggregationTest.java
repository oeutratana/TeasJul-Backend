package com.example.spring_boot_project_api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import com.example.spring_boot_project_api.enums.RoomPublicationStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.enums.RoomTypeStatus;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.RoomType;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.flyway.clean-disabled=true"
})
class RoomRepositoryPublicAggregationTest {

    @Autowired private TestEntityManager entityManager;
    @Autowired private RoomRepository roomRepository;

    @Test
    void groupsOnlyPublishedAvailableRoomsByHouseAndRoomType() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String listingName = "Phase F " + suffix;
        RoomType type = new RoomType();
        type.setOwnerId(1L);
        type.setName("Modern " + suffix);
        type.setCode("PF_" + suffix);
        type.setCapacity(1);
        type.setDefaultRent(BigDecimal.ZERO);
        type.setDefaultDeposit(BigDecimal.ZERO);
        type.setStatus(RoomTypeStatus.ACTIVE);
        RoomType roomType = entityManager.persistFlushFind(type);

        House hA = createHouse("House A " + suffix, "Phnom Penh", null, null, null, null, null, null);
        House hB = createHouse("House B " + suffix, "Phnom Penh", null, null, null, null, null, null);
        long houseA = hA.getId();
        long houseB = hB.getId();
        persistRoom(houseA, roomType.getId(), listingName, "A1-" + suffix,
                RoomStatus.AVAILABLE, RoomPublicationStatus.PUBLISHED, "150.00");
        persistRoom(houseA, roomType.getId(), listingName, "A2-" + suffix,
                RoomStatus.AVAILABLE, RoomPublicationStatus.PUBLISHED, "175.00");
        persistRoom(houseA, roomType.getId(), listingName, "A3-" + suffix,
                RoomStatus.AVAILABLE, RoomPublicationStatus.PUBLISHED, "180.00");
        persistRoom(houseA, roomType.getId(), listingName, "A4-" + suffix,
                RoomStatus.OCCUPIED, RoomPublicationStatus.PUBLISHED, "90.00");
        persistRoom(houseA, roomType.getId(), listingName, "A5-" + suffix,
                RoomStatus.AVAILABLE, RoomPublicationStatus.DRAFT, "80.00");
        persistRoom(houseB, roomType.getId(), listingName, "B1-" + suffix,
                RoomStatus.AVAILABLE, RoomPublicationStatus.PUBLISHED, "160.00");
        entityManager.flush();

        Page<PublicRoomGroupProjection> result = roomRepository.findPublicRoomGroups(
                RoomPublicationStatus.PUBLISHED, RoomStatus.AVAILABLE, listingName,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                false, java.util.List.of(), false, java.util.List.of(), "NEWEST", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).anySatisfy(group -> {
            assertThat(group.getHouseId()).isEqualTo(houseA);
            assertThat(group.getRoomTypeId()).isEqualTo(roomType.getId());
            assertThat(group.getAvailableRoomCount()).isEqualTo(3L);
            assertThat(group.getFromRent()).isEqualByComparingTo("150.00");
        });
        assertThat(result.getContent()).anySatisfy(group -> {
            assertThat(group.getHouseId()).isEqualTo(houseB);
            assertThat(group.getAvailableRoomCount()).isEqualTo(1L);
            assertThat(group.getFromRent()).isEqualByComparingTo("160.00");
        });
    }

    @Test
    void paginatesGroupedResultsAfterAggregation() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String listingName = "Pagination " + suffix;
        RoomType type1 = createRoomType("Type1_" + suffix);
        RoomType type2 = createRoomType("Type2_" + suffix);

        House h1 = createHouse("H1 " + suffix, "Phnom Penh", null, null, null, null, null, null);
        House h2 = createHouse("H2 " + suffix, "Phnom Penh", null, null, null, null, null, null);
        long house1 = h1.getId();
        long house2 = h2.getId();

        // Group 1: House 1, Type 1 (2 rooms)
        persistRoom(house1, type1.getId(), listingName, "P1-" + suffix, RoomStatus.AVAILABLE, RoomPublicationStatus.PUBLISHED, "100.00");
        persistRoom(house1, type1.getId(), listingName, "P2-" + suffix, RoomStatus.AVAILABLE, RoomPublicationStatus.PUBLISHED, "110.00");

        // Group 2: House 1, Type 2 (1 room)
        persistRoom(house1, type2.getId(), listingName, "P3-" + suffix, RoomStatus.AVAILABLE, RoomPublicationStatus.PUBLISHED, "200.00");

        // Group 3: House 2, Type 1 (1 room)
        persistRoom(house2, type1.getId(), listingName, "P4-" + suffix, RoomStatus.AVAILABLE, RoomPublicationStatus.PUBLISHED, "150.00");

        entityManager.flush();

        // Page 0 with size 2 should return 2 groups out of 3 total
        Page<PublicRoomGroupProjection> page0 = roomRepository.findPublicRoomGroups(
                RoomPublicationStatus.PUBLISHED, RoomStatus.AVAILABLE, listingName,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                false, java.util.List.of(), false, java.util.List.of(), "NEWEST", PageRequest.of(0, 2));

        assertThat(page0.getTotalElements()).isEqualTo(3);
        assertThat(page0.getTotalPages()).isEqualTo(2);
        assertThat(page0.getContent()).hasSize(2);

        // Page 1 with size 2 should return 1 group
        Page<PublicRoomGroupProjection> page1 = roomRepository.findPublicRoomGroups(
                RoomPublicationStatus.PUBLISHED, RoomStatus.AVAILABLE, listingName,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                false, java.util.List.of(), false, java.util.List.of(), "NEWEST", PageRequest.of(1, 2));

        assertThat(page1.getContent()).hasSize(1);
    }

    @Test
    void similarRoomsReturnsFourGroupsAndExcludesOnlyCurrentHouseRoomTypePair() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        RoomType currentType = createRoomType("SimilarCurrent_" + suffix);
        RoomType alternateType = createRoomType("SimilarAlternate_" + suffix);
        House currentHouse = createHouse("Similar Current " + suffix, "Phnom Penh", null, null, null, null, null, null);
        House houseB = createHouse("Similar B " + suffix, "Phnom Penh", null, null, null, null, null, null);
        House houseC = createHouse("Similar C " + suffix, "Phnom Penh", null, null, null, null, null, null);
        House houseD = createHouse("Similar D " + suffix, "Phnom Penh", null, null, null, null, null, null);

        persistRoom(currentHouse.getId(), currentType.getId(), suffix, "S-A1-" + suffix,
                RoomStatus.AVAILABLE, RoomPublicationStatus.PUBLISHED, "150.00");
        persistRoom(currentHouse.getId(), currentType.getId(), suffix, "S-A2-" + suffix,
                RoomStatus.AVAILABLE, RoomPublicationStatus.PUBLISHED, "175.00");
        persistRoom(currentHouse.getId(), alternateType.getId(), suffix, "S-A3-" + suffix,
                RoomStatus.AVAILABLE, RoomPublicationStatus.PUBLISHED, "125.00");
        persistRoom(currentHouse.getId(), alternateType.getId(), suffix, "S-A4-" + suffix,
                RoomStatus.AVAILABLE, RoomPublicationStatus.PUBLISHED, "135.00");
        persistRoom(houseB.getId(), currentType.getId(), suffix, "S-B1-" + suffix,
                RoomStatus.AVAILABLE, RoomPublicationStatus.PUBLISHED, "145.00");
        persistRoom(houseB.getId(), currentType.getId(), suffix, "S-B2-" + suffix,
                RoomStatus.AVAILABLE, RoomPublicationStatus.PUBLISHED, "155.00");
        persistRoom(houseC.getId(), alternateType.getId(), suffix, "S-C1-" + suffix,
                RoomStatus.AVAILABLE, RoomPublicationStatus.PUBLISHED, "160.00");
        persistRoom(houseD.getId(), currentType.getId(), suffix, "S-D1-" + suffix,
                RoomStatus.AVAILABLE, RoomPublicationStatus.PUBLISHED, "170.00");
        entityManager.flush();

        // "Similar rooms" is intentionally a global public search across every
        // published + available listing, so on the shared test database the page
        // also contains unrelated groups that can outrank this fixture by price
        // proximity. Query a full page and assert on the groups this test seeded.
        var result = roomRepository.findSimilarRoomGroups(
                currentHouse.getId(), currentType.getId(), new BigDecimal("150.00"),
                RoomPublicationStatus.PUBLISHED, RoomStatus.AVAILABLE, PageRequest.of(0, 1000));

        var fixtureGroups = result.stream()
                .filter(group -> java.util.Set.of(
                        currentHouse.getId(), houseB.getId(), houseC.getId(), houseD.getId())
                        .contains(group.getHouseId()))
                .toList();

        assertThat(fixtureGroups).hasSize(4);
        assertThat(fixtureGroups).noneMatch(group -> currentHouse.getId().equals(group.getHouseId())
                && currentType.getId().equals(group.getRoomTypeId()));
        assertThat(fixtureGroups).extracting(PublicRoomGroupProjection::getHouseId)
                .contains(currentHouse.getId());
        assertThat(fixtureGroups).anySatisfy(group -> {
            assertThat(group.getHouseId()).isEqualTo(currentHouse.getId());
            assertThat(group.getRoomTypeId()).isEqualTo(alternateType.getId());
            assertThat(group.getAvailableRoomCount()).isEqualTo(2L);
            assertThat(group.getFromRent()).isEqualByComparingTo("125.00");
        });
    }

    @Test
    void filtersByCityProvincePropertyTypeAndRentRange() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String listingName = "Filters1 " + suffix;
        RoomType type = createRoomType("TypeF1_" + suffix);

        House phnomPenhHouse = createHouse("PP Villa", "Phnom Penh", com.example.spring_boot_project_api.enums.PropertyType.ROOM, com.example.spring_boot_project_api.enums.Furnishing.FULLY_FURNISHED, 2, 1, new BigDecimal("40.00"), java.time.LocalDate.of(2026, 9, 1));
        House siemReapHouse = createHouse("SR Villa", "Siem Reap", com.example.spring_boot_project_api.enums.PropertyType.ROOM, com.example.spring_boot_project_api.enums.Furnishing.UNFURNISHED, 1, 1, new BigDecimal("20.00"), java.time.LocalDate.of(2026, 9, 1));

        persistRoom(phnomPenhHouse.getId(), type.getId(), listingName, "F1-" + suffix, RoomStatus.AVAILABLE, RoomPublicationStatus.PUBLISHED, "200.00");
        persistRoom(siemReapHouse.getId(), type.getId(), listingName, "F2-" + suffix, RoomStatus.AVAILABLE, RoomPublicationStatus.PUBLISHED, "200.00");
        entityManager.flush();

        // Filter by cityProvince Phnom Penh
        Page<PublicRoomGroupProjection> ppResult = roomRepository.findPublicRoomGroups(
                RoomPublicationStatus.PUBLISHED, RoomStatus.AVAILABLE, listingName,
                "Phnom Penh", null, null, null, null, null, null, null, null, null, null, null, null,
                false, java.util.List.of(), false, java.util.List.of(), "NEWEST", PageRequest.of(0, 10));

        assertThat(ppResult.getTotalElements()).isEqualTo(1);
        assertThat(ppResult.getContent().get(0).getHouseId()).isEqualTo(phnomPenhHouse.getId());
    }

    @Test
    void keepsNullRoomTypeGroupsWhenNoRoomTypeFilter() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String listingName = "NullType " + suffix;
        House house = createHouse("House NT " + suffix, "Phnom Penh", null, null, null, null, null, null);
        persistRoom(house.getId(), null, listingName, "N1-" + suffix,
                RoomStatus.AVAILABLE, RoomPublicationStatus.PUBLISHED, "120.00");
        entityManager.flush();

        Page<PublicRoomGroupProjection> result = roomRepository.findPublicRoomGroups(
                RoomPublicationStatus.PUBLISHED, RoomStatus.AVAILABLE, listingName,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                false, java.util.List.of(), false, java.util.List.of(), "NEWEST", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getHouseId()).isEqualTo(house.getId());
        assertThat(result.getContent().get(0).getRoomTypeId()).isNull();
    }

    @Test
    void filtersGroupsByRoomTypeId() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String listingName = "FilterRT " + suffix;
        RoomType type1 = createRoomType("RT1_" + suffix);
        RoomType type2 = createRoomType("RT2_" + suffix);
        House house = createHouse("House RT " + suffix, "Phnom Penh", null, null, null, null, null, null);
        persistRoom(house.getId(), type1.getId(), listingName, "R1-" + suffix,
                RoomStatus.AVAILABLE, RoomPublicationStatus.PUBLISHED, "100.00");
        persistRoom(house.getId(), type2.getId(), listingName, "R2-" + suffix,
                RoomStatus.AVAILABLE, RoomPublicationStatus.PUBLISHED, "200.00");
        entityManager.flush();

        Page<PublicRoomGroupProjection> result = roomRepository.findPublicRoomGroups(
                RoomPublicationStatus.PUBLISHED, RoomStatus.AVAILABLE, listingName,
                null, null, type2.getId(), null, null, null, null, null, null, null, null, null, null,
                false, java.util.List.of(), false, java.util.List.of(), "NEWEST", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getHouseId()).isEqualTo(house.getId());
        assertThat(result.getContent().get(0).getRoomTypeId()).isEqualTo(type2.getId());
    }

    @Test
    void searchMatchesHouseNameAndRoomTypeName() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String houseName = "Golden " + suffix;
        String typeName = "Deluxe " + suffix;
        RoomType roomType = createNamedRoomType("SRT_" + suffix, typeName);
        House house = createHouse(houseName, "Phnom Penh", null, null, null, null, null, null);
        persistRoom(house.getId(), roomType.getId(), "SearchName " + suffix, "S1-" + suffix,
                RoomStatus.AVAILABLE, RoomPublicationStatus.PUBLISHED, "150.00");
        entityManager.flush();

        Page<PublicRoomGroupProjection> byTypeName = roomRepository.findPublicRoomGroups(
                RoomPublicationStatus.PUBLISHED, RoomStatus.AVAILABLE, typeName,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                false, java.util.List.of(), false, java.util.List.of(), "NEWEST", PageRequest.of(0, 10));

        Page<PublicRoomGroupProjection> byHouseName = roomRepository.findPublicRoomGroups(
                RoomPublicationStatus.PUBLISHED, RoomStatus.AVAILABLE, houseName,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                false, java.util.List.of(), false, java.util.List.of(), "NEWEST", PageRequest.of(0, 10));

        assertThat(byTypeName.getTotalElements()).isEqualTo(1);
        assertThat(byHouseName.getTotalElements()).isEqualTo(1);
        assertThat(byHouseName.getContent().get(0).getHouseId()).isEqualTo(house.getId());
    }

    @Test
    void sortsGroupsByMinimumRentAscendingDescendingAndNewest() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String listingName = "Sort " + suffix;
        RoomType type = createRoomType("ST_" + suffix);
        House houseLow = createHouse("Low " + suffix, "Phnom Penh", null, null, null, null, null, null);
        House houseHigh = createHouse("High " + suffix, "Phnom Penh", null, null, null, null, null, null);
        House houseMid = createHouse("Mid " + suffix, "Phnom Penh", null, null, null, null, null, null);
        persistRoom(houseLow.getId(), type.getId(), listingName, "S1-" + suffix,
                RoomStatus.AVAILABLE, RoomPublicationStatus.PUBLISHED, "100.00", java.time.LocalDateTime.of(2026, 9, 1, 12, 0));
        persistRoom(houseHigh.getId(), type.getId(), listingName, "S2-" + suffix,
                RoomStatus.AVAILABLE, RoomPublicationStatus.PUBLISHED, "300.00", java.time.LocalDateTime.of(2026, 9, 3, 12, 0));
        persistRoom(houseMid.getId(), type.getId(), listingName, "S3-" + suffix,
                RoomStatus.AVAILABLE, RoomPublicationStatus.PUBLISHED, "200.00", java.time.LocalDateTime.of(2026, 9, 2, 12, 0));
        entityManager.flush();

        Page<PublicRoomGroupProjection> asc = roomRepository.findPublicRoomGroups(
                RoomPublicationStatus.PUBLISHED, RoomStatus.AVAILABLE, listingName,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                false, java.util.List.of(), false, java.util.List.of(), "PRICE_ASC", PageRequest.of(0, 10));
        Page<PublicRoomGroupProjection> desc = roomRepository.findPublicRoomGroups(
                RoomPublicationStatus.PUBLISHED, RoomStatus.AVAILABLE, listingName,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                false, java.util.List.of(), false, java.util.List.of(), "PRICE_DESC", PageRequest.of(0, 10));
        Page<PublicRoomGroupProjection> newest = roomRepository.findPublicRoomGroups(
                RoomPublicationStatus.PUBLISHED, RoomStatus.AVAILABLE, listingName,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                false, java.util.List.of(), false, java.util.List.of(), "NEWEST", PageRequest.of(0, 10));

        assertThat(asc.getContent()).extracting(PublicRoomGroupProjection::getFromRent)
                .containsExactly(new BigDecimal("100.00"), new BigDecimal("200.00"), new BigDecimal("300.00"));
        assertThat(desc.getContent()).extracting(PublicRoomGroupProjection::getFromRent)
                .containsExactly(new BigDecimal("300.00"), new BigDecimal("200.00"), new BigDecimal("100.00"));
        assertThat(newest.getContent()).extracting(PublicRoomGroupProjection::getHouseId)
                .containsExactly(houseHigh.getId(), houseMid.getId(), houseLow.getId());
    }

    private RoomType createNamedRoomType(String code, String name) {
        com.example.spring_boot_project_api.model.User owner = entityManager.find(com.example.spring_boot_project_api.model.User.class, 1L);
        if (owner == null) {
            owner = new com.example.spring_boot_project_api.model.User();
            owner.setUsername("owner_" + UUID.randomUUID().toString().substring(0, 6));
            owner.setEmail("owner_" + UUID.randomUUID().toString().substring(0, 6) + "@test.com");
            owner.setPasswordHash("password");
            owner.setFirstName("Owner");
            owner.setLastName("Test");
            owner = entityManager.persistFlushFind(owner);
        }
        RoomType type = new RoomType();
        type.setOwnerId(owner.getId());
        type.setName(name);
        type.setCode(code);
        type.setCapacity(1);
        type.setDefaultRent(BigDecimal.ZERO);
        type.setDefaultDeposit(BigDecimal.ZERO);
        type.setStatus(RoomTypeStatus.ACTIVE);
        return entityManager.persistFlushFind(type);
    }

    private House createHouse(String name, String cityProvince, com.example.spring_boot_project_api.enums.PropertyType propertyType, com.example.spring_boot_project_api.enums.Furnishing furnishing, Integer bedrooms, Integer bathrooms, BigDecimal areaSqm, java.time.LocalDate availableFrom) {
        com.example.spring_boot_project_api.model.User owner = entityManager.find(com.example.spring_boot_project_api.model.User.class, 1L);
        if (owner == null) {
            owner = new com.example.spring_boot_project_api.model.User();
            owner.setUsername("owner_" + UUID.randomUUID().toString().substring(0, 6));
            owner.setEmail("owner_" + UUID.randomUUID().toString().substring(0, 6) + "@test.com");
            owner.setPasswordHash("password");
            owner.setFirstName("Owner");
            owner.setLastName("Test");
            owner = entityManager.persistFlushFind(owner);
        }
        com.example.spring_boot_project_api.model.House house = com.example.spring_boot_project_api.model.House.builder()
                .owner(owner)
                .name(name)
                .code("H_" + UUID.randomUUID().toString().substring(0, 6))
                .slug("slug-" + UUID.randomUUID().toString().substring(0, 6))
                .address("123 Street")
                .cityProvince(cityProvince)
                .propertyType(propertyType)
                .furnishing(furnishing)
                .bedrooms(bedrooms)
                .bathrooms(bathrooms)
                .areaSqm(areaSqm)
                .availableFrom(availableFrom)
                .build();
        return entityManager.persistFlushFind(house);
    }

    private RoomType createRoomType(String code) {
        com.example.spring_boot_project_api.model.User owner = entityManager.find(com.example.spring_boot_project_api.model.User.class, 1L);
        if (owner == null) {
            owner = new com.example.spring_boot_project_api.model.User();
            owner.setUsername("owner_" + UUID.randomUUID().toString().substring(0, 6));
            owner.setEmail("owner_" + UUID.randomUUID().toString().substring(0, 6) + "@test.com");
            owner.setPasswordHash("password");
            owner.setFirstName("Owner");
            owner.setLastName("Test");
            owner = entityManager.persistFlushFind(owner);
        }
        RoomType type = new RoomType();
        type.setOwnerId(owner.getId());
        type.setName("Name " + code);
        type.setCode(code);
        type.setCapacity(1);
        type.setDefaultRent(BigDecimal.ZERO);
        type.setDefaultDeposit(BigDecimal.ZERO);
        type.setStatus(RoomTypeStatus.ACTIVE);
        return entityManager.persistFlushFind(type);
    }

    private void persistRoom(long houseId, Long roomTypeId, String name, String number,
            RoomStatus status, RoomPublicationStatus publicationStatus, String rent) {
        persistRoom(houseId, roomTypeId, name, number, status, publicationStatus, rent,
                LocalDateTime.of(2026, 9, 1, 12, 0));
    }

    private void persistRoom(long houseId, Long roomTypeId, String name, String number,
            RoomStatus status, RoomPublicationStatus publicationStatus, String rent,
            LocalDateTime publishedAt) {
        com.example.spring_boot_project_api.model.User owner = entityManager.find(com.example.spring_boot_project_api.model.User.class, 1L);
        Room room = new Room();
        room.setOwnerId(owner != null ? owner.getId() : 1L);
        room.setHouseId(houseId);
        room.setRoomTypeId(roomTypeId);
        room.setName(name);
        room.setRoomNumber(number);
        room.setCapacity(1);
        room.setMonthlyRent(new BigDecimal(rent));
        room.setSecurityDeposit(BigDecimal.ZERO);
        room.setStatus(status);
        room.setPublicationStatus(publicationStatus);
        room.setPublishedAt(publishedAt);
        entityManager.persist(room);
    }
}
