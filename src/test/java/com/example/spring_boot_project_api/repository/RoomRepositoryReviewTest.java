package com.example.spring_boot_project_api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
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
import com.example.spring_boot_project_api.model.User;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.flyway.clean-disabled=true"
})
class RoomRepositoryReviewTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RoomRepository roomRepository;

    @Test
    void publishedAndAvailable_returnsOnlyPublishedAvailableRooms() {
        String suffix = seedSuffix();
        List<String> numbers = seedRooms(suffix);

        Page<Room> result = roomRepository.findRoomsForReview(
                RoomPublicationStatus.PUBLISHED, RoomStatus.AVAILABLE, PageRequest.of(0, 1000));

        List<String> actual = numbersOf(result);
        assertThat(actual).contains("RV-A-" + suffix);
        assertThat(actual).doesNotContain(
                "RV-O-" + suffix, "RV-M-" + suffix, "RV-R-" + suffix,
                "RV-A-DRAFT-" + suffix, "RV-O-PENDING-" + suffix);
    }

    @Test
    void publishedAndOccupied_returnsOnlyPublishedOccupiedRooms() {
        String suffix = seedSuffix();
        seedRooms(suffix);

        Page<Room> result = roomRepository.findRoomsForReview(
                RoomPublicationStatus.PUBLISHED, RoomStatus.OCCUPIED, PageRequest.of(0, 1000));

        List<String> actual = numbersOf(result);
        assertThat(actual).contains("RV-O-" + suffix);
        assertThat(actual).doesNotContain(
                "RV-A-" + suffix, "RV-M-" + suffix, "RV-R-" + suffix,
                "RV-O-PENDING-" + suffix);
    }

    @Test
    void publishedAndMaintenance_returnsOnlyPublishedMaintenanceRooms() {
        String suffix = seedSuffix();
        seedRooms(suffix);

        Page<Room> result = roomRepository.findRoomsForReview(
                RoomPublicationStatus.PUBLISHED, RoomStatus.MAINTENANCE, PageRequest.of(0, 1000));

        List<String> actual = numbersOf(result);
        assertThat(actual).contains("RV-M-" + suffix);
        assertThat(actual).doesNotContain("RV-A-" + suffix, "RV-O-" + suffix, "RV-R-" + suffix);
    }

    @Test
    void publishedOnly_returnsAllPublishedRoomsRegardlessOfRoomStatus() {
        String suffix = seedSuffix();
        seedRooms(suffix);

        Page<Room> result = roomRepository.findRoomsForReview(
                RoomPublicationStatus.PUBLISHED, null, PageRequest.of(0, 1000));

        // PUBLISHED + OCCUPIED / MAINTENANCE / RESERVED are all valid: the two
        // statuses must stay independent. Only non-PUBLISHED seeds must be absent.
        List<String> actual = numbersOf(result);
        assertThat(actual).contains("RV-A-" + suffix, "RV-O-" + suffix,
                "RV-M-" + suffix, "RV-R-" + suffix);
        assertThat(actual).doesNotContain("RV-A-DRAFT-" + suffix, "RV-O-PENDING-" + suffix);
    }

    @Test
    void noFilters_returnsAllRooms() {
        String suffix = seedSuffix();
        seedRooms(suffix);

        Page<Room> result = roomRepository.findRoomsForReview(null, null, PageRequest.of(0, 1000));

        List<String> actual = numbersOf(result);
        assertThat(actual).contains(
                "RV-A-" + suffix, "RV-O-" + suffix, "RV-M-" + suffix, "RV-R-" + suffix,
                "RV-A-DRAFT-" + suffix, "RV-O-PENDING-" + suffix);
    }

    @Test
    void roomStatusOnly_filtersAcrossAllPublications() {
        String suffix = seedSuffix();
        seedRooms(suffix);

        Page<Room> result = roomRepository.findRoomsForReview(
                null, RoomStatus.OCCUPIED, PageRequest.of(0, 1000));

        List<String> actual = numbersOf(result);
        assertThat(actual).contains("RV-O-" + suffix, "RV-O-PENDING-" + suffix);
        assertThat(actual).doesNotContain("RV-A-" + suffix, "RV-M-" + suffix, "RV-R-" + suffix);
    }

    @Test
    void pagination_stillAppliesWhenFilteringByRoomStatus() {
        String suffix = seedSuffix();
        seedRooms(suffix);

        Page<Room> page0 = roomRepository.findRoomsForReview(
                RoomPublicationStatus.PUBLISHED, null, PageRequest.of(0, 2));
        Page<Room> page1 = roomRepository.findRoomsForReview(
                RoomPublicationStatus.PUBLISHED, null, PageRequest.of(1, 2));

        // Count query keeps working on the shared database and pages stay consistent.
        assertThat(page0.getTotalElements()).isGreaterThanOrEqualTo(4);
        assertThat(page0.getTotalPages()).isEqualTo((int) Math.ceil((double) page0.getTotalElements() / 2.0));
        assertThat(page0.getContent()).hasSize(2);
        assertThat(page1.getContent()).hasSize(2);

        List<Long> page0Ids = page0.getContent().stream().map(Room::getId).toList();
        List<Long> page1Ids = page1.getContent().stream().map(Room::getId).toList();
        assertThat(page0Ids).doesNotContainAnyElementsOf(page1Ids);
    }

    private String seedSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Seeds six rooms (status, publicationStatus) all sharing {@code suffix}:
     * <ul>
     *   <li>RV-A-&lt;suffix&gt;      AVAILABLE,   PUBLISHED</li>
     *   <li>RV-O-&lt;suffix&gt;      OCCUPIED,    PUBLISHED</li>
     *   <li>RV-M-&lt;suffix&gt;      MAINTENANCE, PUBLISHED</li>
     *   <li>RV-R-&lt;suffix&gt;      RESERVED,    PUBLISHED</li>
     *   <li>RV-A-DRAFT-&lt;suffix&gt;      AVAILABLE,   DRAFT</li>
     *   <li>RV-O-PENDING-&lt;suffix&gt;    OCCUPIED,    PENDING_APPROVAL</li>
     * </ul>
     * Returns the room numbers.
     */
    private List<String> seedRooms(String suffix) {
        User owner = entityManager.find(User.class, 1L);
        if (owner == null) {
            owner = new User();
            owner.setUsername("owner_" + suffix);
            owner.setEmail("owner_" + suffix + "@test.com");
            owner.setPasswordHash("password");
            owner.setFirstName("Owner");
            owner.setLastName("Test");
            owner = entityManager.persistFlushFind(owner);
        }
        RoomType type = new RoomType();
        type.setOwnerId(owner.getId());
        type.setName("Review " + suffix);
        type.setCode("RV_" + suffix);
        type.setCapacity(1);
        type.setDefaultRent(BigDecimal.ZERO);
        type.setDefaultDeposit(BigDecimal.ZERO);
        type.setStatus(RoomTypeStatus.ACTIVE);
        RoomType roomType = entityManager.persistFlushFind(type);

        House house = House.builder()
                .owner(owner)
                .name("House " + suffix)
                .code("H_" + suffix)
                .slug("slug-" + suffix)
                .address("123 Street")
                .cityProvince("Phnom Penh")
                .build();
        House persisted = entityManager.persistFlushFind(house);

        RoomStatus[] roomStatuses = {
                RoomStatus.AVAILABLE, RoomStatus.OCCUPIED, RoomStatus.MAINTENANCE,
                RoomStatus.RESERVED, RoomStatus.AVAILABLE, RoomStatus.OCCUPIED
        };
        RoomPublicationStatus[] publicationStatuses = {
                RoomPublicationStatus.PUBLISHED, RoomPublicationStatus.PUBLISHED,
                RoomPublicationStatus.PUBLISHED, RoomPublicationStatus.PUBLISHED,
                RoomPublicationStatus.DRAFT, RoomPublicationStatus.PENDING_APPROVAL
        };
        String[] numbers = {
                "RV-A-" + suffix, "RV-O-" + suffix, "RV-M-" + suffix,
                "RV-R-" + suffix, "RV-A-DRAFT-" + suffix, "RV-O-PENDING-" + suffix
        };

        for (int i = 0; i < roomStatuses.length; i++) {
            Room room = new Room();
            room.setOwnerId(owner.getId());
            room.setHouseId(persisted.getId());
            room.setRoomTypeId(roomType.getId());
            room.setName("Room " + numbers[i]);
            room.setRoomNumber(numbers[i]);
            room.setCapacity(1);
            room.setMonthlyRent(new BigDecimal("120.00"));
            room.setSecurityDeposit(BigDecimal.ZERO);
            room.setStatus(roomStatuses[i]);
            room.setPublicationStatus(publicationStatuses[i]);
            entityManager.persistFlushFind(room);
        }
        return List.of(numbers);
    }

    private List<String> numbersOf(Page<Room> page) {
        return page.getContent().stream().map(Room::getRoomNumber).toList();
    }
}