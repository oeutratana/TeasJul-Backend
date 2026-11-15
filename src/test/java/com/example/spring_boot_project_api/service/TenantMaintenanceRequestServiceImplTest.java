package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.spring_boot_project_api.dto.request.CreateTenantMaintenanceRequestRequest;
import com.example.spring_boot_project_api.dto.response.MaintenanceRequestResponse;
import com.example.spring_boot_project_api.enums.RentalContractStatus;
import com.example.spring_boot_project_api.exception.InvalidMaintenanceOperationException;
import com.example.spring_boot_project_api.mapper.MaintenanceRequestMapper;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.MaintenanceRequest;
import com.example.spring_boot_project_api.model.MaintenanceRequestPriority;
import com.example.spring_boot_project_api.model.MaintenanceRequestStatus;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.model.TenantStatus;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserStatus;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.MaintenanceCategoryRepository;
import com.example.spring_boot_project_api.repository.MaintenanceRequestRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.impl.MaintenanceRequestServiceImpl;
import com.example.spring_boot_project_api.service.impl.TenantContextResolver;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
class TenantMaintenanceRequestServiceImplTest {

    @Mock
    private MaintenanceRequestRepository requestRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private HouseRepository houseRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private MaintenanceCategoryRepository categoryRepository;
    @Spy
    private MaintenanceRequestMapper requestMapper = new MaintenanceRequestMapper();
    @Mock
    private TenantContextResolver tenantContextResolver;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private MaintenanceRequestServiceImpl maintenanceRequestService;

    private User tenantUser;
    private User owner;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenantUser = User.builder().id(10L).username("sopha").email("sopha@example.com")
                .passwordHash("h").firstName("Sopha").lastName("Chan").status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        owner = User.builder().id(1L).username("ownerA").email("ownerA@example.com")
                .passwordHash("h").firstName("Owner").lastName("A").status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        tenant = Tenant.builder().id(50L).user(tenantUser).owner(owner).tenantCode("TN-A01")
                .firstName("Sopha").lastName("Chan").phone("012000111").status(TenantStatus.RENTING)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authAs(User user) {
        UserDetails details = org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .roles("USER")
                .build();
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(details, "n/a"));
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
    }

    private Room room() {
        Room room = new Room();
        room.setId(12L);
        room.setOwnerId(1L);
        room.setHouseId(8L);
        room.setRoomNumber("A101");
        return room;
    }

    private House house() {
        return House.builder().id(8L).owner(owner).name("Golden House").code("GH-01")
                .address("Phnom Penh").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    private RentalContract activeContract() {
        return RentalContract.builder().id(900L).contractNo("C-001").ownerId(1L).roomId(12L)
                .startDate(LocalDate.now().minusMonths(1)).endDate(LocalDate.now().plusMonths(11))
                .monthlyRent(new BigDecimal("150000")).dueDay(5).status(RentalContractStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    @Test
    void getMyRequestsAsTenantReturnsOnlyLinkedTenantRequests() {
        authAs(tenantUser);
        when(tenantContextResolver.requireLinkedTenant(10L)).thenReturn(tenant);

        MaintenanceRequest existing = MaintenanceRequest.builder().id(77L).title("Fix AC").build();
        existing.setOwner(owner);
        existing.setRoom(room());
        existing.setHouse(house());
        existing.setTenant(tenant);
        existing.setRequestNo("MR-001");
        existing.setPriority(MaintenanceRequestPriority.HIGH);
        existing.setStatus(MaintenanceRequestStatus.PENDING);

        when(requestRepository.findAllByTenantIdOrderByCreatedAtDesc(50L)).thenReturn(List.of(existing));

        List<MaintenanceRequestResponse> responses = maintenanceRequestService.getMyRequestsAsTenant();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).tenantId()).isEqualTo(50L);
        verify(requestRepository).findAllByTenantIdOrderByCreatedAtDesc(50L);
    }

    @Test
    void createMyRequestResolvesOwnerHouseRoomFromActiveContract() {
        authAs(tenantUser);
        when(tenantContextResolver.requireLinkedTenant(10L)).thenReturn(tenant);
        when(tenantContextResolver.activeContractOf(50L)).thenReturn(activeContract());
        when(roomRepository.findById(12L)).thenReturn(Optional.of(room()));
        when(houseRepository.findById(8L)).thenReturn(Optional.of(house()));
        when(requestRepository.countByOwnerId(1L)).thenReturn(3L);
        when(requestRepository.existsByOwnerIdAndRequestNo(anyLong(), anyString())).thenReturn(false);
        when(requestRepository.save(any(MaintenanceRequest.class))).thenAnswer(inv -> {
            MaintenanceRequest r = inv.getArgument(0);
            r.setId(80L);
            return r;
        });

        MaintenanceRequestResponse response = maintenanceRequestService.createMyRequest(
                new CreateTenantMaintenanceRequestRequest("Leaking pipe", "Kitchen sink leaks",
                        MaintenanceRequestPriority.URGENT));

        assertThat(response.id()).isEqualTo(80L);
        assertThat(response.title()).isEqualTo("Leaking pipe");
        assertThat(response.ownerId()).isEqualTo(1L);
        assertThat(response.houseId()).isEqualTo(8L);
        assertThat(response.roomId()).isEqualTo(12L);
        assertThat(response.tenantId()).isEqualTo(50L);
        assertThat(response.priority()).isEqualTo(MaintenanceRequestPriority.URGENT);
        assertThat(response.status()).isEqualTo(MaintenanceRequestStatus.PENDING);
        assertThat(response.requestNo()).startsWith("MR-");
    }

    @Test
    void createMyRequestRejectsTenantWithoutActiveContract() {
        authAs(tenantUser);
        when(tenantContextResolver.requireLinkedTenant(10L)).thenReturn(tenant);
        when(tenantContextResolver.activeContractOf(50L)).thenReturn(null);

        assertThatThrownBy(() -> maintenanceRequestService.createMyRequest(
                new CreateTenantMaintenanceRequestRequest("Leaking pipe", null, null)))
                .isInstanceOf(InvalidMaintenanceOperationException.class);
        verify(requestRepository, never()).save(any());
    }
}