package com.example.spring_boot_project_api.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.RentalContractRequest;
import com.example.spring_boot_project_api.dto.request.UpdateOwnerContractInfoRequest;
import com.example.spring_boot_project_api.dto.request.UpdateOwnerSettingsRequest;
import com.example.spring_boot_project_api.dto.response.RentalContractResponse;
import com.example.spring_boot_project_api.enums.RentalContractStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.enums.RoomTypeStatus;
import com.example.spring_boot_project_api.model.Gender;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Role;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.RoomType;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserRole;
import com.example.spring_boot_project_api.model.UserRoleId;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.OwnerSettingRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoleRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.RoomTypeRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.UserRoleRepository;
import com.example.spring_boot_project_api.service.OwnerSettingService;
import com.example.spring_boot_project_api.service.RentalContractService;
import com.example.spring_boot_project_api.util.SecurityUtils;

/**
 * Proves the Party A snapshot against the real {@code rental_contracts} table, not
 * a mock: the owner's settings are saved, a contract is created, and the row is
 * re-read from the database to confirm all eight columns were written.
 */
@SpringBootTest(properties = "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
@Transactional
class OwnerContractPartySnapshotEndToEndTest {

    @Autowired private RentalContractService rentalContractService;
    @Autowired private OwnerSettingService ownerSettingService;
    @Autowired private UserRepository userRepository;
    @Autowired private HouseRepository houseRepository;
    @Autowired private RoomTypeRepository roomTypeRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RentalContractRepository rentalContractRepository;
    @Autowired private OwnerSettingRepository ownerSettingRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRoleRepository userRoleRepository;

    private User owner;
    private House house;
    private RoomType roomType;
    private Room roomA;
    private Room roomB;

    @BeforeEach
    void setUp() {
        String token = UUID.randomUUID().toString().substring(0, 8);
        owner = User.builder()
                .username("snap_owner_" + token)
                .email("snap_owner_" + token + "@example.com")
                .passwordHash("repro")
                .firstName("van")
                .lastName("roth")
                .phone("02434242342")
                .build();
        owner = userRepository.save(owner);

        house = House.builder()
                .owner(owner)
                .name("Snap House " + token)
                .slug("snap-house-" + token)
                .code("SH" + token)
                .address("Repro Address")
                .build();
        house = houseRepository.save(house);

        roomType = new RoomType();
        roomType.setOwnerId(owner.getId());
        roomType.setName("Snap Type " + token);
        roomType.setCode("ST" + token);
        roomType.setStatus(RoomTypeStatus.ACTIVE);
        roomType = roomTypeRepository.save(roomType);

        roomA = room("A" + token);
        roomB = room("B" + token);

        // Permissions are resolved from the database, not the security token, so
        // the owner needs a real OWNER role row.
        Role ownerRole = roleRepository.findByCode(SecurityUtils.ROLE_OWNER).orElseThrow();
        userRoleRepository.save(new UserRole(new UserRoleId(owner.getId(), ownerRole.getId()),
                owner, ownerRole, LocalDateTime.now()));

        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User(owner.getUsername(), "repro", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_OWNER"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Room room(String number) {
        Room r = new Room();
        r.setOwnerId(owner.getId());
        r.setHouseId(house.getId());
        r.setRoomTypeId(roomType.getId());
        r.setRoomNumber(number);
        r.setMonthlyRent(new BigDecimal("100.00"));
        r.setCapacity(1);
        r.setStatus(RoomStatus.AVAILABLE);
        return roomRepository.save(r);
    }

    private UpdateOwnerSettingsRequest settingsWith(UpdateOwnerContractInfoRequest party) {
        return new UpdateOwnerSettingsRequest(
                "USD", "USD", new BigDecimal("4100"), "DD/MM/YYYY", "Asia/Phnom_Penh",
                "Business", "", "", "", "", "#3b82f6", "#10b981", "KM",
                "INV", 10, "", true, true, true, true,
                "កិច្ចសន្យាជួលផ្ទះ", "", "", "", "", "KM", party,
                "RCP", "KM", "", true, true, true, true, "", "");
    }

    private RentalContractRequest contractRequest(Long roomId) {
        RentalContractRequest r = new RentalContractRequest();
        r.setRoomId(roomId);
        r.setStartDate(LocalDate.now().minusMonths(1));
        r.setEndDate(LocalDate.now().plusMonths(6));
        r.setMonthlyRent(new BigDecimal("100.00"));
        r.setSecurityDeposit(new BigDecimal("50.00"));
        r.setDueDay(5);
        r.setStatus(RentalContractStatus.ACTIVE);
        return r;
    }

    @Test
    void allEightPartyAFieldsAreWrittenToTheRentalContractsRow() {
        ownerSettingService.updateSettings(settingsWith(new UpdateOwnerContractInfoRequest(
                "van roth", Gender.MALE, LocalDate.of(1990, 4, 12), "Cambodian",
                "N012345678", "Phnom Penh", "02434242342", "Teacher")));

        RentalContractResponse response = rentalContractService.createContract(
                contractRequest(roomA.getId()));

        // Re-read straight from the database: this is the column-level proof.
        RentalContract stored = rentalContractRepository.findById(response.getId()).orElseThrow();

        assertThat(stored.getOwnerFullName()).isEqualTo("van roth");
        assertThat(stored.getOwnerGender()).isEqualTo("MALE");
        assertThat(stored.getOwnerDateOfBirth()).isEqualTo(LocalDate.of(1990, 4, 12));
        assertThat(stored.getOwnerNationality()).isEqualTo("Cambodian");
        assertThat(stored.getOwnerNationalId()).isEqualTo("N012345678");
        assertThat(stored.getOwnerAddress()).isEqualTo("Phnom Penh");
        assertThat(stored.getOwnerPhone()).isEqualTo("02434242342");
        assertThat(stored.getOwnerOccupation()).isEqualTo("Teacher");

        // And the API response carries the same values.
        assertThat(response.getOwnerFullName()).isEqualTo("van roth");
        assertThat(response.getOwnerGender()).isEqualTo("MALE");
        assertThat(response.getOwnerNationality()).isEqualTo("Cambodian");
        assertThat(response.getOwnerNationalId()).isEqualTo("N012345678");
        assertThat(response.getOwnerAddress()).isEqualTo("Phnom Penh");
        assertThat(response.getOwnerPhone()).isEqualTo("02434242342");
        assertThat(response.getOwnerOccupation()).isEqualTo("Teacher");
    }

    @Test
    void blankOptionalFieldsAreStoredAsNullNotFabricated() {
        ownerSettingService.updateSettings(settingsWith(new UpdateOwnerContractInfoRequest(
                "van roth", null, null, null, null, null, "02434242342", null)));

        RentalContractResponse response = rentalContractService.createContract(
                contractRequest(roomA.getId()));

        RentalContract stored = rentalContractRepository.findById(response.getId()).orElseThrow();
        assertThat(stored.getOwnerFullName()).isEqualTo("van roth");
        assertThat(stored.getOwnerPhone()).isEqualTo("02434242342");
        assertThat(stored.getOwnerGender()).isNull();
        assertThat(stored.getOwnerDateOfBirth()).isNull();
        assertThat(stored.getOwnerNationality()).isNull();
        assertThat(stored.getOwnerNationalId()).isNull();
        assertThat(stored.getOwnerAddress()).isNull();
        assertThat(stored.getOwnerOccupation()).isNull();
    }

    @Test
    void existingContractKeepsItsSnapshotWhenOwnerSettingsChange() {
        ownerSettingService.updateSettings(settingsWith(new UpdateOwnerContractInfoRequest(
                "van roth", Gender.MALE, null, "Cambodian", "N012345678", "Phnom Penh",
                "02434242342", "Teacher")));

        RentalContractResponse contractA = rentalContractService.createContract(
                contractRequest(roomA.getId()));

        // The owner edits their settings afterwards.
        ownerSettingService.updateSettings(settingsWith(new UpdateOwnerContractInfoRequest(
                "van roth", Gender.MALE, null, "Cambodian", "N099999999", "Phnom Penh",
                "099999999", "Teacher")));

        RentalContract reloadedA = rentalContractRepository.findById(contractA.getId()).orElseThrow();
        assertThat(reloadedA.getOwnerPhone()).isEqualTo("02434242342");
        assertThat(reloadedA.getOwnerNationalId()).isEqualTo("N012345678");

        RentalContractResponse contractB = rentalContractService.createContract(
                contractRequest(roomB.getId()));
        RentalContract storedB = rentalContractRepository.findById(contractB.getId()).orElseThrow();
        assertThat(storedB.getOwnerPhone()).isEqualTo("099999999");
        assertThat(storedB.getOwnerNationalId()).isEqualTo("N099999999");
    }
}
