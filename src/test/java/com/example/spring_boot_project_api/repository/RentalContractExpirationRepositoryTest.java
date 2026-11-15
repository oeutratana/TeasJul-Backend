package com.example.spring_boot_project_api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import com.example.spring_boot_project_api.enums.RentalContractStatus;
import com.example.spring_boot_project_api.enums.RoomPublicationStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserStatus;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.flyway.clean-disabled=true"
})
class RentalContractExpirationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RentalContractRepository rentalContractRepository;

    @Test
    void contractWithEndDateBeforeTodayBecomesExpiredWhileTodayAndFutureStayActive() {
        LocalDate today = LocalDate.now();
        Room room = persistRoom();
        RentalContract past = persistContract(room, today.minusDays(1), RentalContractStatus.ACTIVE);
        RentalContract todayContract = persistContract(room, today, RentalContractStatus.ACTIVE);
        RentalContract future = persistContract(room, today.plusDays(30), RentalContractStatus.ACTIVE);
        RentalContract draftPast = persistContract(room, today.minusDays(1), RentalContractStatus.DRAFT);
        RentalContract noEndDate = persistContract(room, null, RentalContractStatus.ACTIVE);

        int updated = rentalContractRepository.expireActiveContractsEndedBefore(
                RentalContractStatus.ACTIVE, RentalContractStatus.EXPIRED, today);

        assertThat(updated).isGreaterThanOrEqualTo(1);
        assertThat(rentalContractRepository.findById(past.getId()).orElseThrow().getStatus())
                .isEqualTo(RentalContractStatus.EXPIRED);
        assertThat(rentalContractRepository.findById(todayContract.getId()).orElseThrow().getStatus())
                .isEqualTo(RentalContractStatus.ACTIVE);
        assertThat(rentalContractRepository.findById(future.getId()).orElseThrow().getStatus())
                .isEqualTo(RentalContractStatus.ACTIVE);
        assertThat(rentalContractRepository.findById(draftPast.getId()).orElseThrow().getStatus())
                .isEqualTo(RentalContractStatus.DRAFT);
        assertThat(rentalContractRepository.findById(noEndDate.getId()).orElseThrow().getStatus())
                .isEqualTo(RentalContractStatus.ACTIVE);

        List<RentalContract> active = rentalContractRepository.findByStatus(RentalContractStatus.ACTIVE);
        assertThat(active).extracting(RentalContract::getId)
                .contains(todayContract.getId(), future.getId(), noEndDate.getId())
                .doesNotContain(past.getId(), draftPast.getId());
    }

    @Test
    void findRoomIdsOfActiveExpiredContractsReturnsDistinctRoomsWithPastDueActiveContracts() {
        LocalDate today = LocalDate.now();
        Room roomA = persistRoom();
        Room roomB = persistRoom();
        persistContract(roomA, today.minusDays(1), RentalContractStatus.ACTIVE);
        persistContract(roomA, today.minusDays(2), RentalContractStatus.ACTIVE);
        persistContract(roomA, today.minusDays(1), RentalContractStatus.DRAFT);
        persistContract(roomB, today.minusDays(1), RentalContractStatus.ACTIVE);
        persistContract(roomB, today, RentalContractStatus.ACTIVE);

        List<Long> roomIds = rentalContractRepository.findRoomIdsOfActiveExpiredContracts(
                RentalContractStatus.ACTIVE, today);

        assertThat(roomIds).contains(roomA.getId(), roomB.getId()).doesNotHaveDuplicates();
    }

    @Test
    void existsByContractNoDetectsExistingNumber() {
        Room room = persistRoom();
        String contractNo = "CT-" + UUID.randomUUID().toString().substring(0, 8);
        persistContract(room, LocalDate.now().plusMonths(1), RentalContractStatus.ACTIVE)
                .setContractNo(contractNo);
        entityManager.flush();

        assertThat(rentalContractRepository.existsByContractNo(contractNo)).isTrue();
        assertThat(rentalContractRepository.existsByContractNo("NO-SUCH-" + UUID.randomUUID())).isFalse();
    }

    @Test
    void expiredContractRenewedToTodayOrFutureIsNotExpiredByExpirationPass() {
        LocalDate today = LocalDate.now();
        Room room = persistRoom();
        RentalContract renewedToday = persistContract(room, today.minusDays(30), RentalContractStatus.EXPIRED);
        RentalContract renewedFuture = persistContract(room, today.minusDays(30), RentalContractStatus.EXPIRED);

        renewedToday.setEndDate(today);
        renewedToday.setStatus(RentalContractStatus.ACTIVE);
        renewedFuture.setEndDate(today.plusMonths(6));
        renewedFuture.setStatus(RentalContractStatus.ACTIVE);
        entityManager.flush();

        rentalContractRepository.expireActiveContractsEndedBefore(
                RentalContractStatus.ACTIVE, RentalContractStatus.EXPIRED, today);
        entityManager.clear();

        assertThat(rentalContractRepository.findById(renewedToday.getId()).orElseThrow().getStatus())
                .isEqualTo(RentalContractStatus.ACTIVE);
        assertThat(rentalContractRepository.findById(renewedFuture.getId()).orElseThrow().getStatus())
                .isEqualTo(RentalContractStatus.ACTIVE);
    }

    private Room persistRoom() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User owner = User.builder()
                .username("owner_" + suffix)
                .email("owner_" + suffix + "@example.com")
                .passwordHash("$2a$10$somehashedvaluehere")
                .firstName("Owner")
                .lastName(suffix)
                .status(UserStatus.ACTIVE)
                .build();
        owner = entityManager.persistFlushFind(owner);

        House house = House.builder()
                .owner(owner)
                .name("House " + suffix)
                .slug("house-" + suffix)
                .code("H-" + suffix)
                .address("1 Street " + suffix)
                .build();
        house = entityManager.persistFlushFind(house);

        Room room = new Room();
        room.setOwnerId(owner.getId());
        room.setHouseId(house.getId());
        room.setRoomNumber("R-" + suffix);
        room.setName("Room " + suffix);
        room.setFloorNumber(1);
        room.setMonthlyRent(new BigDecimal("100.00"));
        room.setSecurityDeposit(new BigDecimal("50.00"));
        room.setCapacity(1);
        room.setStatus(RoomStatus.AVAILABLE);
        room.setPublicationStatus(RoomPublicationStatus.DRAFT);
        return entityManager.persistFlushFind(room);
    }

    private RentalContract persistContract(Room room, LocalDate endDate, RentalContractStatus status) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        RentalContract contract = RentalContract.builder()
                .ownerId(room.getOwnerId())
                .contractNo("RC-" + suffix)
                .roomId(room.getId())
                .startDate(LocalDate.now().minusMonths(1))
                .endDate(endDate)
                .monthlyRent(new BigDecimal("100.00"))
                .securityDeposit(new BigDecimal("50.00"))
                .dueDay(5)
                .status(status)
                .build();
        return entityManager.persistFlushFind(contract);
    }
}