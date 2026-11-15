package com.example.spring_boot_project_api.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.spring_boot_project_api.dto.request.ContractTenantRequest;
import com.example.spring_boot_project_api.dto.response.ContractTenantResponse;
import com.example.spring_boot_project_api.enums.RentalContractStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.enums.RoomTypeStatus;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.RoomType;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.model.TenantStatus;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.ContractTenantRepository;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.RoomTypeRepository;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.ContractTenantService;

@SpringBootTest(properties = "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
class ContractTenantEndToEndTest {

    @Autowired private ContractTenantService contractTenantService;
    @Autowired private UserRepository userRepository;
    @Autowired private HouseRepository houseRepository;
    @Autowired private RoomTypeRepository roomTypeRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RentalContractRepository rentalContractRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private ContractTenantRepository contractTenantRepository;

    private User owner;
    private User tenantUser;
    private House house;
    private RoomType roomType;
    private Room room;
    private RentalContract contract;
    private Tenant tenant;
    private Long contractTenantId;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        if (contractTenantId != null) {
            contractTenantRepository.deleteById(contractTenantId);
        }
        if (tenant != null) {
            tenantRepository.delete(tenant);
        }
        if (contract != null) {
            rentalContractRepository.delete(contract);
        }
        if (room != null) {
            roomRepository.delete(room);
        }
        if (roomType != null) {
            roomTypeRepository.delete(roomType);
        }
        if (house != null) {
            houseRepository.delete(house);
        }
        if (tenantUser != null) {
            userRepository.delete(tenantUser);
        }
        if (owner != null) {
            userRepository.delete(owner);
        }
    }

    @Test
    void addTenantToContract_tenantWithoutLinkedUser() {
        setupFixtures(false);

        ContractTenantResponse response = contractTenantService.addTenantToContract(
                new ContractTenantRequest(contract.getId(), tenant.getId(), false, LocalDate.now(), null));

        contractTenantId = response.id();
        assertThat(response.tenantId()).isEqualTo(tenant.getId());
    }

    @Test
    void addTenantToContract_tenantWithLinkedUser() {
        setupFixtures(true);

        ContractTenantResponse response = contractTenantService.addTenantToContract(
                new ContractTenantRequest(contract.getId(), tenant.getId(), false, LocalDate.now(), null));

        contractTenantId = response.id();
        assertThat(response.tenantId()).isEqualTo(tenant.getId());
    }

    private void setupFixtures(boolean tenantHasUser) {
        String token = UUID.randomUUID().toString().substring(0, 8);
        owner = User.builder()
                .username("repro_owner_" + token)
                .email("repro_owner_" + token + "@example.com")
                .passwordHash("repro")
                .firstName("Repro")
                .lastName("Owner")
                .build();
        owner = userRepository.save(owner);

        house = House.builder()
                .owner(owner)
                .name("Repro House " + token)
                .slug("repro-house-" + token)
                .code("RH" + token)
                .address("Repro Address")
                .build();
        house = houseRepository.save(house);

        roomType = new RoomType();
        roomType.setOwnerId(owner.getId());
        roomType.setName("Repro Type " + token);
        roomType.setCode("RT" + token);
        roomType.setStatus(RoomTypeStatus.ACTIVE);
        roomType = roomTypeRepository.save(roomType);

        room = new Room();
        room.setOwnerId(owner.getId());
        room.setHouseId(house.getId());
        room.setRoomTypeId(roomType.getId());
        room.setRoomNumber("R" + token);
        room.setMonthlyRent(new BigDecimal("100.00"));
        room.setCapacity(1);
        room.setStatus(RoomStatus.AVAILABLE);
        room = roomRepository.save(room);

        contract = RentalContract.builder()
                .ownerId(owner.getId())
                .contractNo("C-" + token)
                .roomId(room.getId())
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(6))
                .monthlyRent(new BigDecimal("100.00"))
                .securityDeposit(new BigDecimal("100.00"))
                .dueDay(5)
                .status(RentalContractStatus.DRAFT)
                .build();
        contract = rentalContractRepository.save(contract);

        User tenantUser = null;
        if (tenantHasUser) {
            tenantUser = User.builder()
                    .username("repro_tenant_" + token)
                    .email("repro_tenant_" + token + "@example.com")
                    .passwordHash("repro")
                    .firstName("Repro")
                    .lastName("Tenant")
                    .build();
            this.tenantUser = userRepository.save(tenantUser);
        }

        tenant = Tenant.builder()
                .user(tenantUser)
                .owner(owner)
                .house(house)
                .tenantCode("T" + token)
                .firstName("Repro")
                .lastName("Tenant")
                .phone("000-" + token)
                .status(TenantStatus.ACTIVE)
                .build();
        tenant = tenantRepository.save(tenant);

        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User(owner.getUsername(), "repro", java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, java.util.List.of()));
    }
}