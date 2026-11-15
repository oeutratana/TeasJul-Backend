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

import com.example.spring_boot_project_api.enums.ContractDepositStatus;
import com.example.spring_boot_project_api.enums.RentalContractStatus;
import com.example.spring_boot_project_api.enums.RoomPublicationStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.model.ContractDeposit;
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
class ContractDepositRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ContractDepositRepository depositRepository;

    @Test
    void sumPaidAmountByHouseIdsSumsOnlyPaidDepositsAcrossContractsOfTheOwner() {
        Room first = persistRoomWithHouse();
        RentalContract contractA = persistContract(persistRoom(first.getOwnerId(), first.getHouseId()));
        RentalContract contractB = persistContract(persistRoom(first.getOwnerId(), first.getHouseId()));

        persistDeposit(contractA, ContractDepositStatus.PAID, "150.00");
        persistDeposit(contractB, ContractDepositStatus.PAID, "200.00");
        persistDeposit(contractA, ContractDepositStatus.PENDING, "150.00");

        BigDecimal sum = depositRepository.sumPaidAmountByHouseIds(List.of(first.getHouseId()));

        assertThat(sum).isEqualByComparingTo("350.00");
    }

    @Test
    void sumPaidAmountByHouseIdsExcludesDepositsBelongingToOtherHouses() {
        Room ownerRoom = persistRoomWithHouse();
        Room otherRoom = persistRoomWithHouse();
        RentalContract ownerContract = persistContract(persistRoom(ownerRoom.getOwnerId(), ownerRoom.getHouseId()));
        RentalContract otherContract = persistContract(persistRoom(otherRoom.getOwnerId(), otherRoom.getHouseId()));

        persistDeposit(ownerContract, ContractDepositStatus.PAID, "100.00");
        persistDeposit(otherContract, ContractDepositStatus.PAID, "500.00");

        BigDecimal sum = depositRepository.sumPaidAmountByHouseIds(List.of(ownerRoom.getHouseId()));

        assertThat(sum).isEqualByComparingTo("100.00");
    }

    @Test
    void sumPaidAmountByHouseIdsIgnoresPendingAndRefundedDeposits() {
        Room ownerRoom = persistRoomWithHouse();
        RentalContract contract = persistContract(persistRoom(ownerRoom.getOwnerId(), ownerRoom.getHouseId()));

        persistDeposit(contract, ContractDepositStatus.PENDING, "300.00");
        persistDeposit(contract, ContractDepositStatus.REFUNDED, "300.00");
        persistDeposit(contract, ContractDepositStatus.PARTIALLY_REFUNDED, "150.00");

        BigDecimal sum = depositRepository.sumPaidAmountByHouseIds(List.of(ownerRoom.getHouseId()));

        assertThat(sum).isEqualByComparingTo("0");
    }

    @Test
    void sumPaidAmountByHouseIdsReturnsZeroWhenNoPaidDepositsExist() {
        Room ownerRoom = persistRoomWithHouse();
        persistContract(persistRoom(ownerRoom.getOwnerId(), ownerRoom.getHouseId()));

        BigDecimal sum = depositRepository.sumPaidAmountByHouseIds(List.of(ownerRoom.getHouseId()));

        assertThat(sum).isEqualByComparingTo("0");
    }

    @Test
    void sumPaidAmountByHouseIdsReturnsZeroForUnknownHouses() {
        BigDecimal sum = depositRepository.sumPaidAmountByHouseIds(List.of(999999L));

        assertThat(sum).isEqualByComparingTo("0");
    }

    private Room persistRoomWithHouse() {
        User owner = User.builder()
                .username("owner_" + UUID.randomUUID().toString().substring(0, 8))
                .email("owner_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com")
                .passwordHash("$2a$10$somehashedvaluehere")
                .firstName("Owner")
                .lastName(UUID.randomUUID().toString().substring(0, 8))
                .status(UserStatus.ACTIVE)
                .build();
        owner = entityManager.persistFlushFind(owner);

        House house = House.builder()
                .owner(owner)
                .name("House " + UUID.randomUUID().toString().substring(0, 8))
                .slug("house-" + UUID.randomUUID().toString().substring(0, 8))
                .code("H-" + UUID.randomUUID().toString().substring(0, 8))
                .address("1 Street " + UUID.randomUUID().toString().substring(0, 8))
                .build();
        house = entityManager.persistFlushFind(house);

        return persistRoom(owner.getId(), house.getId());
    }

    private Room persistRoom(Long ownerId, Long houseId) {
        Room room = new Room();
        room.setOwnerId(ownerId);
        room.setHouseId(houseId);
        room.setRoomNumber("R-" + UUID.randomUUID().toString().substring(0, 8));
        room.setName("Room " + UUID.randomUUID().toString().substring(0, 8));
        room.setFloorNumber(1);
        room.setMonthlyRent(new BigDecimal("100.00"));
        room.setSecurityDeposit(new BigDecimal("50.00"));
        room.setCapacity(1);
        room.setStatus(RoomStatus.AVAILABLE);
        room.setPublicationStatus(RoomPublicationStatus.DRAFT);
        return entityManager.persistFlushFind(room);
    }

    private RentalContract persistContract(Room room) {
        RentalContract contract = RentalContract.builder()
                .ownerId(room.getOwnerId())
                .contractNo("RC-" + UUID.randomUUID().toString().substring(0, 8))
                .roomId(room.getId())
                .startDate(LocalDate.now().minusMonths(1))
                .endDate(LocalDate.now().plusMonths(1))
                .monthlyRent(new BigDecimal("100.00"))
                .securityDeposit(new BigDecimal("50.00"))
                .dueDay(5)
                .status(RentalContractStatus.ACTIVE)
                .build();
        return entityManager.persistFlushFind(contract);
    }

    private ContractDeposit persistDeposit(RentalContract contract,
            ContractDepositStatus status, String paidAmount) {
        BigDecimal amount = new BigDecimal(paidAmount);
        ContractDeposit deposit = ContractDeposit.builder()
                .rentalContract(contract)
                .amount(amount)
                .paidAmount(status == ContractDepositStatus.PAID ? amount : BigDecimal.ZERO)
                .status(status)
                .build();
        return entityManager.persistFlushFind(deposit);
    }
}