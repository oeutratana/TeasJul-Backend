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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.spring_boot_project_api.dto.request.UpdateUserRequest;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.UserResponse;
import com.example.spring_boot_project_api.dto.response.UserSummaryResponse;
import com.example.spring_boot_project_api.dto.response.UserTenantCandidateResponse;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.mapper.UserMapper;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserStatus;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.impl.UserServiceImpl;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final Long OWNER_ID = 1L;
    private static final Long SUPER_ADMIN_ID = 3L;
    private static final Long MY_HOUSE_ID = 10L;
    private static final Long MY_TENANT_USER_ID = 25L;
    private static final Long OTHER_OWNER_TENANT_USER_ID = 77L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private HouseAccessService houseAccessService;

    @Spy
    private UserMapper userMapper = new UserMapper();

    @InjectMocks
    private UserServiceImpl userService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /** Authenticates the caller as a plain OWNER owning {@link #MY_HOUSE_ID}. */
    private void stubOwnerScope() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(true);
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(OWNER_ID);
        when(houseAccessService.getScopedOwnerIds()).thenReturn(List.of(OWNER_ID));
        when(houseAccessService.getAccessibleHouseIds()).thenReturn(List.of(MY_HOUSE_ID));
    }

    private void stubSuperAdmin() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
    }

    /** No staff role at all: used by the "must be rejected" cases. */
    private void stubNoStaffRole() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);
    }

    private User user() {
        return User.builder()
                .id(1L)
                .username("johndoe")
                .email("john@example.com")
                .passwordHash("hashed")
                .firstName("John")
                .lastName("Doe")
                .phone("+1234567890")
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private UserDetails principal() {
        return org.springframework.security.core.userdetails.User.builder()
                .username("johndoe")
                .password("n/a")
                .roles("USER")
                .build();
    }

    @Test
    void getUserByIdShouldReturnUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        when(securityUtils.getCurrentUserId()).thenReturn(1L);

        UserResponse response = userService.getUserById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("johndoe");
    }

    @Test
    void getUserByIdShouldThrowWhenMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        when(securityUtils.getCurrentUserId()).thenReturn(99L);

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getCurrentUserShouldReturnAuthenticatedUser() {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(principal(), "n/a"));
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(user()));

        UserResponse response = userService.getCurrentUser();

        assertThat(response.username()).isEqualTo("johndoe");
    }

    @Test
    void getAllUsersShouldReturnPagedUsers() {
        Page<User> page = new PageImpl<>(List.of(user()), PageRequest.of(0, 10), 1);
        when(userRepository.findAllWithFilters(eq(null), eq(null), any(Pageable.class))).thenReturn(page);
        stubSuperAdmin();

        PageResponse<UserResponse> users = userService.getAllUsers(null, null, PageRequest.of(0, 10));

        assertThat(users.content()).hasSize(1);
        assertThat(users.content().get(0).email()).isEqualTo("john@example.com");
        assertThat(users.totalElements()).isEqualTo(1);
    }

    @Test
    void getAllUsersCapsPageSize() {
        Page<User> page = new PageImpl<>(List.of(user()), PageRequest.of(0, 10), 1);
        when(userRepository.findAllWithFilters(eq(null), eq(null), any(Pageable.class))).thenReturn(page);
        stubSuperAdmin();

        userService.getAllUsers(null, null, PageRequest.of(0, 99999));

        org.mockito.ArgumentCaptor<Pageable> captor =
                org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAllWithFilters(eq(null), eq(null), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(200);
    }

    @Test
    void getAllUsersRequiresOwnerRole() {
        stubNoStaffRole();

        assertThatThrownBy(() -> userService.getAllUsers(null, null, PageRequest.of(0, 10)))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void getUsersSummaryGroupsByStatus() {
        stubSuperAdmin();
        when(userRepository.countGroupedByStatus())
                .thenReturn(List.of(
                        new Object[]{UserStatus.ACTIVE, 3L},
                        new Object[]{UserStatus.LOCKED, 1L}));

        UserSummaryResponse summary = userService.getUsersSummary();

        assertThat(summary.total()).isEqualTo(4L);
        assertThat(summary.statusCounts()).containsEntry("ACTIVE", 3L);
        assertThat(summary.statusCounts()).containsEntry("LOCKED", 1L);
    }

    @Test
    void updateUserShouldOnlyOverwriteProvidedFields() {
        User existing = user();
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(securityUtils.getCurrentUserId()).thenReturn(1L);

        UpdateUserRequest request = new UpdateUserRequest("Johnny", null, "+9876543210", null);

        UserResponse response = userService.updateUser(1L, request);

        assertThat(response.firstName()).isEqualTo("Johnny");
        assertThat(response.lastName()).isEqualTo("Doe");
        assertThat(response.phone()).isEqualTo("+9876543210");
    }

    @Test
    void deleteUserShouldRemoveUser() {
        User existing = user();
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        stubSuperAdmin();

        userService.deleteUser(1L);

        org.mockito.ArgumentCaptor<User> captor =
                org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).delete(captor.capture());
        assertThat(captor.getValue()).isSameAs(existing);
    }

    // ---- Search available for tenant ----

    private User candidate() {
        return User.builder()
                .id(25L)
                .username("darasok")
                .email("dara@gmail.com")
                .passwordHash("hashed")
                .firstName("Dara")
                .lastName("Sok")
                .phone("012345678")
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void searchAvailableForTenantReturnsPagedProjectsAndTrimsQuery() {
        Page<User> page = new PageImpl<>(List.of(candidate()), PageRequest.of(0, 10), 1);
        stubSuperAdmin();
        when(userRepository.findAvailableForTenant(eq("dara"), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<UserTenantCandidateResponse> response =
                userService.searchAvailableForTenant("  dara  ", PageRequest.of(0, 10));

        assertThat(response.content()).hasSize(1);
        UserTenantCandidateResponse item = response.content().get(0);
        assertThat(item.id()).isEqualTo(25L);
        assertThat(item.firstName()).isEqualTo("Dara");
        assertThat(item.lastName()).isEqualTo("Sok");
        assertThat(item.email()).isEqualTo("dara@gmail.com");
        assertThat(item.phone()).isEqualTo("012345678");
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(10);
    }

    @Test
    void searchAvailableForTenantExposesNoSensitiveFields() {
        Page<User> page = new PageImpl<>(List.of(candidate()), PageRequest.of(0, 10), 1);
        stubSuperAdmin();
        when(userRepository.findAvailableForTenant(eq("dara"), any(Pageable.class))).thenReturn(page);

        UserTenantCandidateResponse item =
                userService.searchAvailableForTenant("dara", PageRequest.of(0, 10)).content().get(0);

        assertThat(item.username()).isNotBlank();
        // passwordHash / tokens etc. are simply not part of the DTO
    }

    @Test
    void searchAvailableForTenantCapsPageSize() {
        User one = candidate();
        User two = candidate();
        two.setId(26L);
        Page<User> page = new PageImpl<>(List.of(one, two), PageRequest.of(0, 10), 2);
        stubSuperAdmin();
        when(userRepository.findAvailableForTenant(eq("x"), any(Pageable.class))).thenReturn(page);

        PageResponse<UserTenantCandidateResponse> response =
                userService.searchAvailableForTenant("x", PageRequest.of(0, 99999));

        assertThat(response.content()).hasSize(2);

        org.mockito.ArgumentCaptor<Pageable> captor =
                org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAvailableForTenant(eq("x"), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    void searchAvailableForTenantEmptyQueryPassedAsNullDoesNotCrash() {
        stubSuperAdmin();
        when(userRepository.findAvailableForTenant(eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        PageResponse<UserTenantCandidateResponse> response =
                userService.searchAvailableForTenant(null, PageRequest.of(0, 10));

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
    }

    // ------------------------------------------------------------------
    // OWNER authorization scope
    // ------------------------------------------------------------------

    @Test
    void getAllUsersAsOwnerUsesOwnerScopedQueryAndNeverTheGlobalOne() {
        Page<User> page = new PageImpl<>(List.of(user()), PageRequest.of(0, 10), 1);
        stubOwnerScope();
        when(userRepository.findAllInOwnerScope(eq(null), eq(null), eq(OWNER_ID),
                anyCollection(), anyCollection(), any(Pageable.class))).thenReturn(page);

        userService.getAllUsers(null, null, PageRequest.of(0, 10));

        verify(userRepository, never()).findAllWithFilters(any(), any(), any(Pageable.class));
        verify(userRepository).findAllInOwnerScope(eq(null), eq(null), eq(OWNER_ID),
                eq(List.of(OWNER_ID)), eq(List.of(MY_HOUSE_ID)), any(Pageable.class));
    }

    @Test
    void getAllUsersAsSuperAdminStaysUnrestricted() {
        Page<User> page = new PageImpl<>(List.of(user()), PageRequest.of(0, 10), 1);
        stubSuperAdmin();
        when(userRepository.findAllWithFilters(any(), any(), any(Pageable.class))).thenReturn(page);

        userService.getAllUsers(null, null, PageRequest.of(0, 10));

        verify(userRepository).findAllWithFilters(any(), any(), any(Pageable.class));
        verify(userRepository, never()).findAllInOwnerScope(any(), any(), anyLong(),
                anyCollection(), anyCollection(), any(Pageable.class));
    }

    @Test
    void getUsersSummaryAsOwnerUsesScopedCount() {
        stubOwnerScope();
        when(userRepository.countGroupedByStatusInOwnerScope(eq(OWNER_ID), anyCollection(), anyCollection()))
                .thenReturn(List.<Object[]>of(new Object[]{UserStatus.ACTIVE, 2L}));

        UserSummaryResponse summary = userService.getUsersSummary();

        assertThat(summary.total()).isEqualTo(2L);
        verify(userRepository, never()).countGroupedByStatus();
    }

    @Test
    void ownerCanViewSelfWithoutScopeLookup() {
        when(securityUtils.getCurrentUserId()).thenReturn(OWNER_ID);
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(user()));

        assertThat(userService.getUserById(OWNER_ID).id()).isEqualTo(OWNER_ID);
        verify(userRepository, never()).existsInOwnerScope(anyLong(), anyLong(),
                anyCollection(), anyCollection());
    }

    @Test
    void ownerCanViewOwnTenantUser() {
        stubOwnerScope();
        when(userRepository.existsInOwnerScope(eq(MY_TENANT_USER_ID), eq(OWNER_ID),
                anyCollection(), anyCollection())).thenReturn(true);
        User tenantUser = user();
        tenantUser.setId(MY_TENANT_USER_ID);
        when(userRepository.findById(MY_TENANT_USER_ID)).thenReturn(Optional.of(tenantUser));

        assertThat(userService.getUserById(MY_TENANT_USER_ID).id()).isEqualTo(MY_TENANT_USER_ID);
    }

    @Test
    void ownerCannotViewAnotherOwnersTenantUser() {
        stubOwnerScope();
        when(userRepository.existsInOwnerScope(eq(OTHER_OWNER_TENANT_USER_ID), eq(OWNER_ID),
                anyCollection(), anyCollection())).thenReturn(false);

        assertThatThrownBy(() -> userService.getUserById(OTHER_OWNER_TENANT_USER_ID))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verify(userRepository, never()).findById(OTHER_OWNER_TENANT_USER_ID);
    }

    @Test
    void ownerCannotViewSuperAdminUser() {
        stubOwnerScope();
        when(userRepository.existsInOwnerScope(eq(SUPER_ADMIN_ID), eq(OWNER_ID),
                anyCollection(), anyCollection())).thenReturn(false);

        assertThatThrownBy(() -> userService.getUserById(SUPER_ADMIN_ID))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verify(userRepository, never()).findById(SUPER_ADMIN_ID);
    }

    @Test
    void ownerCannotUpdateAnotherOwnersTenantUser() {
        stubOwnerScope();
        when(userRepository.existsInOwnerScope(eq(OTHER_OWNER_TENANT_USER_ID), eq(OWNER_ID),
                anyCollection(), anyCollection())).thenReturn(false);

        UpdateUserRequest request = new UpdateUserRequest("Hacked", null, null, null);

        assertThatThrownBy(() -> userService.updateUser(OTHER_OWNER_TENANT_USER_ID, request))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void ownerCannotDeleteAnotherOwnersTenantUser() {
        stubOwnerScope();
        when(userRepository.existsInOwnerScope(eq(OTHER_OWNER_TENANT_USER_ID), eq(OWNER_ID),
                anyCollection(), anyCollection())).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(OTHER_OWNER_TENANT_USER_ID))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void ownerCannotDeleteSuperAdminUser() {
        stubOwnerScope();
        when(userRepository.existsInOwnerScope(eq(SUPER_ADMIN_ID), eq(OWNER_ID),
                anyCollection(), anyCollection())).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(SUPER_ADMIN_ID))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void ownerCanDeleteOwnTenantUser() {
        stubOwnerScope();
        when(userRepository.existsInOwnerScope(eq(MY_TENANT_USER_ID), eq(OWNER_ID),
                anyCollection(), anyCollection())).thenReturn(true);
        User tenantUser = user();
        tenantUser.setId(MY_TENANT_USER_ID);
        when(userRepository.findById(MY_TENANT_USER_ID)).thenReturn(Optional.of(tenantUser));

        userService.deleteUser(MY_TENANT_USER_ID);

        verify(userRepository).delete(tenantUser);
    }

    @Test
    void requireUserManagementAccessAsSuperAdminIsUnrestricted() {
        stubSuperAdmin();

        userService.requireUserManagementAccess(OTHER_OWNER_TENANT_USER_ID);

        verify(userRepository, never()).existsInOwnerScope(anyLong(), anyLong(),
                anyCollection(), anyCollection());
    }

    @Test
    void requireUserManagementAccessRejectsNonStaffCaller() {
        stubNoStaffRole();

        assertThatThrownBy(() -> userService.requireUserManagementAccess(MY_TENANT_USER_ID))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void searchAvailableForTenantAsOwnerUsesScopedQuery() {
        stubOwnerScope();
        when(userRepository.findAvailableForTenantInOwnerScope(eq("dara"), eq(OWNER_ID),
                anyCollection(), anyCollection(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        userService.searchAvailableForTenant("dara", PageRequest.of(0, 10));

        verify(userRepository, never()).findAvailableForTenant(any(), any(Pageable.class));
        verify(userRepository).findAvailableForTenantInOwnerScope(eq("dara"), eq(OWNER_ID),
                eq(List.of(OWNER_ID)), eq(List.of(MY_HOUSE_ID)), any(Pageable.class));
    }

    @Test
    void searchAvailableForTenantRejectsTenantRole() {
        stubNoStaffRole();
        when(securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)).thenReturn(false);

        assertThatThrownBy(() -> userService.searchAvailableForTenant("dara", PageRequest.of(0, 10)))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verify(userRepository, never()).findAvailableForTenant(any(), any(Pageable.class));
        verify(userRepository, never()).findAvailableForTenantInOwnerScope(any(), anyLong(),
                anyCollection(), anyCollection(), any(Pageable.class));
    }

    @Test
    void searchAvailableForTenantAsManagerUsesScopedQueryAndNeverTheGlobalOne() {
        Long managerId = 2L;
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);
        when(securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)).thenReturn(true);
        when(securityUtils.getCurrentUserId()).thenReturn(managerId);
        when(houseAccessService.getScopedOwnerIds()).thenReturn(List.of(OWNER_ID));
        when(houseAccessService.getAccessibleHouseIds()).thenReturn(List.of(MY_HOUSE_ID));
        when(userRepository.findAvailableForTenantInOwnerScope(eq("dara"), eq(managerId),
                anyCollection(), anyCollection(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        userService.searchAvailableForTenant("dara", PageRequest.of(0, 10));

        verify(userRepository, never()).findAvailableForTenant(any(), any(Pageable.class));
        verify(userRepository).findAvailableForTenantInOwnerScope(eq("dara"), eq(managerId),
                eq(List.of(OWNER_ID)), eq(List.of(MY_HOUSE_ID)), any(Pageable.class));
    }
}
