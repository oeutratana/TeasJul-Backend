package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.example.spring_boot_project_api.dto.request.CreateTenantRequest;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.TenantResponse;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.mapper.TenantMapper;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.HouseStatus;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.model.TenantStatus;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserStatus;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.impl.TenantServiceImpl;
import com.example.spring_boot_project_api.util.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class TenantServiceImplTest {

    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserHouseAccessRepository userHouseAccessRepository;
    @Spy
    private TenantMapper tenantMapper = new TenantMapper();
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private HouseAccessService houseAccessService;

    @InjectMocks
    private TenantServiceImpl tenantService;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).username("ownerA").email("ownerA@example.com")
                .passwordHash("h").firstName("Owner").lastName("A").status(UserStatus.ACTIVE)
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

    private User candidateUser() {
        return User.builder().id(25L).username("darasok").email("dara@gmail.com")
                .passwordHash("h").firstName("Dara").lastName("Sok").phone("012345678")
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    private User manager() {
        return User.builder().id(2L).username("managerA").email("managerA@example.com")
                .passwordHash("h").firstName("Manager").lastName("A").status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    private CreateTenantRequest request(Long userId) {
        return new CreateTenantRequest(
                userId,
                "TN-100",
                "Dara",
                "Sok",
                null,
                null,
                "012345678",
                "dara@gmail.com",
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private Tenant withOwner(User owner, User linkedUser) {
        Tenant tenant = tenantMapper.toEntity(request(linkedUser != null ? linkedUser.getId() : null));
        tenant.setId(7L);
        tenant.setUser(linkedUser);
        tenant.setOwner(owner);
        tenant.setStatus(TenantStatus.ACTIVE);
        return tenant;
    }

    @Test
    void createTenantLinksExistingUserWithinScope() {
        authAs(owner);
        User candidate = candidateUser();
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(owner.getId());
        when(houseAccessService.getScopedOwnerIds()).thenReturn(List.of(owner.getId()));
        when(houseAccessService.getAccessibleHouseIds()).thenReturn(List.of(10L));
        when(userRepository.existsInOwnerScope(eq(25L), eq(owner.getId()),
                anyCollection(), anyCollection())).thenReturn(true);
        when(userRepository.findById(25L)).thenReturn(Optional.of(candidate));
        when(tenantRepository.findByUserId(25L)).thenReturn(Optional.empty());
        when(tenantRepository.existsByOwnerIdAndTenantCode(1L, "TN-100")).thenReturn(false);
        when(tenantRepository.existsByOwnerIdAndPhone(1L, "012345678")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            t.setId(7L);
            return t;
        });

        TenantResponse response = tenantService.createTenant(request(25L));

        assertThat(response.userId()).isEqualTo(25L);
        assertThat(response.ownerId()).isEqualTo(1L);
        assertThat(response.tenantCode()).isEqualTo("TN-100");
    }

    @Test
    void createTenantCannotLinkUserOutsideCallerScope() {
        authAs(owner);
        User candidate = candidateUser();
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(owner.getId());
        when(houseAccessService.getScopedOwnerIds()).thenReturn(List.of(owner.getId()));
        when(houseAccessService.getAccessibleHouseIds()).thenReturn(List.of(10L));
        when(userRepository.findById(25L)).thenReturn(Optional.of(candidate));
        when(userRepository.existsInOwnerScope(eq(25L), eq(owner.getId()),
                anyCollection(), anyCollection())).thenReturn(false);

        assertThatThrownBy(() -> tenantService.createTenant(request(25L)))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verify(tenantRepository, never()).save(any());
    }

    @Test
    void createTenantManagerCanLinkUserWithinManagedScope() {
        User manager = manager();
        authAs(manager);
        User candidate = candidateUser();
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(manager.getId());
        when(houseAccessService.getScopedOwnerIds()).thenReturn(List.of(owner.getId()));
        when(houseAccessService.getAccessibleHouseIds()).thenReturn(List.of(10L));
        when(userRepository.existsInOwnerScope(eq(25L), eq(manager.getId()),
                anyCollection(), anyCollection())).thenReturn(true);
        when(userRepository.findById(25L)).thenReturn(Optional.of(candidate));
        when(tenantRepository.findByUserId(25L)).thenReturn(Optional.empty());
        when(tenantRepository.existsByOwnerIdAndTenantCode(2L, "TN-100")).thenReturn(false);
        when(tenantRepository.existsByOwnerIdAndPhone(2L, "012345678")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        TenantResponse response = tenantService.createTenant(request(25L));

        assertThat(response.userId()).isEqualTo(25L);
        assertThat(response.ownerId()).isEqualTo(2L);
    }

    @Test
    void createTenantManagerCannotLinkUserOutsideManagedScope() {
        User manager = manager();
        authAs(manager);
        User candidate = candidateUser();
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(manager.getId());
        when(houseAccessService.getScopedOwnerIds()).thenReturn(List.of());
        when(houseAccessService.getAccessibleHouseIds()).thenReturn(List.of());
        when(userRepository.findById(25L)).thenReturn(Optional.of(candidate));
        when(userRepository.existsInOwnerScope(eq(25L), eq(manager.getId()),
                anyCollection(), anyCollection())).thenReturn(false);

        assertThatThrownBy(() -> tenantService.createTenant(request(25L)))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verify(tenantRepository, never()).save(any());
    }

    @Test
    void createTenantSuperAdminCanLinkAnyUser() {
        authAs(owner);
        User candidate = candidateUser();
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
        when(userRepository.findById(25L)).thenReturn(Optional.of(candidate));
        when(tenantRepository.findByUserId(25L)).thenReturn(Optional.empty());
        when(tenantRepository.existsByOwnerIdAndTenantCode(1L, "TN-100")).thenReturn(false);
        when(tenantRepository.existsByOwnerIdAndPhone(1L, "012345678")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        TenantResponse response = tenantService.createTenant(request(25L));

        assertThat(response.userId()).isEqualTo(25L);
        verify(userRepository, never()).existsInOwnerScope(anyLong(), anyLong(),
                anyCollection(), anyCollection());
    }

    @Test
    void createTenantAllowsNoUserAccount() {
        authAs(owner);
        when(tenantRepository.existsByOwnerIdAndTenantCode(1L, "TN-100")).thenReturn(false);
        when(tenantRepository.existsByOwnerIdAndPhone(1L, "012345678")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            t.setId(7L);
            return t;
        });

        TenantResponse response = tenantService.createTenant(request(null));

        assertThat(response.userId()).isNull();
        assertThat(response.ownerId()).isEqualTo(1L);
        verify(userRepository, never()).findById(any());
    }

    @Test
    void createTenantRejectsForeignUserId() {
        authAs(owner);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.createTenant(request(999L)))
                .isInstanceOf(UserNotFoundException.class);
        verify(tenantRepository, never()).save(any());
    }

    @Test
    void createTenantRejectsUserAlreadyLinkedWithoutDisclosingUsername() {
        authAs(owner);
        User candidate = candidateUser();
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
        when(userRepository.findById(25L)).thenReturn(Optional.of(candidate));
        when(tenantRepository.findByUserId(25L)).thenReturn(Optional.of(Tenant.builder().id(1L).build()));

        assertThatThrownBy(() -> tenantService.createTenant(request(25L)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageNotContaining(candidate.getUsername());
        verify(tenantRepository, never()).save(any());
    }

    @Test
    void createTenantOwnerAlwaysComesFromSecurityContext() {
        // The request has no ownerId field at all, so client-supplied owner scope
        // is impossible by design; owner must resolve to the authenticated user.
        authAs(owner);
        when(tenantRepository.existsByOwnerIdAndTenantCode(1L, "TN-100")).thenReturn(false);
        when(tenantRepository.existsByOwnerIdAndPhone(1L, "012345678")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        TenantResponse response = tenantService.createTenant(request(null));

        assertThat(response.ownerId()).isEqualTo(1L);
        verify(tenantRepository).existsByOwnerIdAndTenantCode(1L, "TN-100");
    }

    @Test
    void noHouseOrRoomIsRequiredToCreateATenant() {
        authAs(owner);
        when(tenantRepository.existsByOwnerIdAndTenantCode(1L, "TN-100")).thenReturn(false);
        when(tenantRepository.existsByOwnerIdAndPhone(1L, "012345678")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        TenantResponse response = tenantService.createTenant(request(null));

        assertThat(response).isNotNull();
        verify(tenantRepository).existsByOwnerIdAndTenantCode(1L, "TN-100");
    }

    @Test
    void tenantMapperHandlesNullUserWhenMappingResponse() {
        Tenant tenant = withOwner(owner, null);
        tenant.setId(7L);

        TenantResponse response = tenantMapper.toResponse(tenant);

        assertThat(response.userId()).isNull();
        assertThat(response.id()).isEqualTo(7L);
    }

    @Test
    void getMyTenantsReturnsOnlyTenantsInvitedByCurrentManager() {
        User managerA = User.builder().id(2L).username("managerA").email("managerA@example.com")
                .passwordHash("h").firstName("Manager").lastName("A").status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        House house = House.builder().id(10L).owner(owner).name("Golden House")
                .slug("golden-house").code("GH-1").address("PP").status(HouseStatus.ACTIVE).build();

        Tenant tenantA = withOwner(owner, null);
        tenantA.setId(7L);
        tenantA.setHouse(house);
        tenantA.setInvitedBy(managerA);
        tenantA.setStatus(TenantStatus.PENDING);

        when(securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)).thenReturn(true);
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);
        when(houseAccessService.getAccessibleHouseIds()).thenReturn(List.of(10L));
        Page<Tenant> page = new PageImpl<>(List.of(tenantA), PageRequest.of(0, 10), 1);
        when(tenantRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<TenantResponse> responses = tenantService.getMyTenants(null, null, PageRequest.of(0, 10));

        assertThat(responses.content()).hasSize(1);
        assertThat(responses.content().get(0).id()).isEqualTo(7L);
        assertThat(responses.content().get(0).invitedById()).isEqualTo(managerA.getId());
    }

    @Test
    void getMyTenantsReturnsAllTenantsForOwner() {
        Tenant tenantA = withOwner(owner, null);
        tenantA.setId(7L);
        Tenant tenantB = withOwner(owner, null);
        tenantB.setId(8L);

        when(securityUtils.getCurrentUserId()).thenReturn(owner.getId());
        when(securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)).thenReturn(false);
        Page<Tenant> page = new PageImpl<>(List.of(tenantA, tenantB), PageRequest.of(0, 10), 2);
        when(tenantRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<TenantResponse> responses = tenantService.getMyTenants(null, null, PageRequest.of(0, 10));

        assertThat(responses.content()).hasSize(2);
        assertThat(responses.content()).extracting(TenantResponse::id).containsExactly(7L, 8L);
        assertThat(responses.totalElements()).isEqualTo(2);
    }

    @Test
    void getTenantByIdAllowsManagerWithHouseAccess() {
        User managerA = User.builder().id(2L).build();
        House house = House.builder().id(10L).owner(owner).name("Golden House")
                .slug("golden-house").code("GH-1").address("PP").status(HouseStatus.ACTIVE).build();
        Tenant tenant = withOwner(owner, null);
        tenant.setId(7L);
        tenant.setHouse(house);

        when(securityUtils.getCurrentUserId()).thenReturn(managerA.getId());
        when(tenantRepository.findById(7L)).thenReturn(Optional.of(tenant));
        when(houseAccessService.canAccessHouse(10L)).thenReturn(true);

        TenantResponse response = tenantService.getTenantById(7L);
        assertThat(response.id()).isEqualTo(7L);
    }

    @Test
    void getTenantByIdDisallowsOtherManager() {
        User managerB = User.builder().id(3L).build();
        House house = House.builder().id(10L).owner(owner).name("Golden House")
                .slug("golden-house").code("GH-1").address("PP").status(HouseStatus.ACTIVE).build();
        Tenant tenant = withOwner(owner, null);
        tenant.setId(7L);
        tenant.setHouse(house);

        when(securityUtils.getCurrentUserId()).thenReturn(managerB.getId());
        when(tenantRepository.findById(7L)).thenReturn(Optional.of(tenant));
        when(houseAccessService.canAccessHouse(10L)).thenReturn(false);

        org.junit.jupiter.api.Assertions.assertThrows(
                com.example.spring_boot_project_api.exception.TenantNotFoundException.class,
                () -> tenantService.getTenantById(7L));
    }
}