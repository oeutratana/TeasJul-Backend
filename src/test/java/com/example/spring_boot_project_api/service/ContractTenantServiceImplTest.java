package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.spring_boot_project_api.dto.request.ContractTenantRequest;
import com.example.spring_boot_project_api.dto.response.ContractTenantResponse;
import com.example.spring_boot_project_api.enums.RentalContractStatus;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.mapper.ContractTenantMapper;
import com.example.spring_boot_project_api.model.ContractTenant;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.model.TenantStatus;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserStatus;
import com.example.spring_boot_project_api.repository.ContractTenantRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.service.impl.ContractTenantServiceImpl;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
class ContractTenantServiceImplTest {

    @Mock
    private ContractTenantRepository contractTenantRepository;
    @Mock
    private RentalContractRepository contractRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Spy
    private ContractTenantMapper contractTenantMapper = new ContractTenantMapper();
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private HouseAccessService houseAccessService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ContractTenantServiceImpl contractTenantService;

    private User owner() {
        return User.builder().id(1L).username("ownerA").passwordHash("h")
                .firstName("Owner").lastName("A").status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    private Tenant tenantWith(User owner, TenantStatus status) {
        return Tenant.builder().id(50L).user(null).owner(owner).tenantCode("TN-A01")
                .firstName("Sopha").lastName("Chan").phone("012000111").status(status)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    private RentalContract activeContract() {
        return RentalContract.builder().id(900L).contractNo("C-001").ownerId(1L).roomId(12L)
                .startDate(LocalDate.now().minusMonths(1)).endDate(LocalDate.now().plusMonths(11))
                .monthlyRent(new BigDecimal("150000")).dueDay(5).status(RentalContractStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    private ContractTenantRequest request() {
        return new ContractTenantRequest(900L, 50L, true, LocalDate.now(), null);
    }

    private void stubHappyPath(Tenant tenant) {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(contractRepository.findById(900L)).thenReturn(java.util.Optional.of(activeContract()));
        when(houseAccessService.resolveContractHouseId(900L)).thenReturn(12L);
        when(tenantRepository.findByIdAndOwnerId(50L, 1L)).thenReturn(java.util.Optional.of(tenant));
        when(tenantRepository.findById(50L)).thenReturn(java.util.Optional.of(tenant));
        when(contractTenantMapper.toEntity(any())).thenReturn(
                ContractTenant.builder().id(7L).contractId(900L).tenantId(50L).isPrimary(true)
                        .joinedAt(LocalDate.now()).build());
        when(contractTenantRepository.save(any(ContractTenant.class))).thenAnswer(inv -> {
            ContractTenant entity = inv.getArgument(0);
            entity.setId(7L);
            return entity;
        });
        when(contractTenantMapper.toResponse(any())).thenReturn(
                new ContractTenantResponse(7L, 900L, 50L, true, LocalDate.now(), null,
                        LocalDateTime.now(), LocalDateTime.now()));
    }

    @Test
    void addTenantAllowsPendingTenantFromInvitationFlow() {
        Tenant tenant = tenantWith(owner(), TenantStatus.PENDING);
        stubHappyPath(tenant);
        when(contractTenantRepository.isTenantActivelyRenting(eq(50L), any(LocalDate.class)))
                .thenReturn(false).thenReturn(true);

        ContractTenantResponse response = contractTenantService.addTenantToContract(request());

        assertThat(response.tenantId()).isEqualTo(50L);
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.RENTING);
    }

    @Test
    void addTenantAllowsActiveTenant() {
        Tenant tenant = tenantWith(owner(), TenantStatus.ACTIVE);
        stubHappyPath(tenant);
        when(contractTenantRepository.isTenantActivelyRenting(eq(50L), any(LocalDate.class)))
                .thenReturn(false).thenReturn(true);

        ContractTenantResponse response = contractTenantService.addTenantToContract(request());

        assertThat(response.tenantId()).isEqualTo(50L);
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.RENTING);
    }

    @Test
    void addTenantRejectsInactiveTenant() {
        Tenant tenant = tenantWith(owner(), TenantStatus.INACTIVE);
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(contractRepository.findById(900L)).thenReturn(java.util.Optional.of(activeContract()));
        when(houseAccessService.resolveContractHouseId(900L)).thenReturn(12L);
        when(tenantRepository.findByIdAndOwnerId(50L, 1L)).thenReturn(java.util.Optional.of(tenant));

        assertThatThrownBy(() -> contractTenantService.addTenantToContract(request()))
                .isInstanceOf(DuplicateResourceException.class);

        verify(contractTenantRepository, never()).save(any());
        verify(contractTenantMapper, never()).toEntity(any());
    }
}