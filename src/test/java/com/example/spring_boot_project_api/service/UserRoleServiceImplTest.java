package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.example.spring_boot_project_api.dto.response.RoleResponse;
import com.example.spring_boot_project_api.exception.InvalidEmailException;
import com.example.spring_boot_project_api.mapper.RoleMapper;
import com.example.spring_boot_project_api.model.Role;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.RoleRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.UserRoleRepository;
import com.example.spring_boot_project_api.service.impl.UserRoleServiceImpl;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
class UserRoleServiceImplTest {

    @Mock private UserRoleRepository userRoleRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private RoleMapper roleMapper;
    @Mock private SecurityUtils securityUtils;
    @Mock private UserService userService;

    @InjectMocks private UserRoleServiceImpl userRoleService;

    private User user(String email) {
        return User.builder().id(7L).username("manager001").email(email).build();
    }

    private Role role(String code) {
        return Role.builder().id(2L).code(code).name(code).build();
    }

    private void stubOwner() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(true);
    }

    @Test
    void assignManagerRoleToRentalComEmailIsAllowed() {
        stubOwner();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user("manager001@rental.com")));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role("MANAGER")));
        when(userRoleRepository.existsByUserIdAndRoleId(7L, 2L)).thenReturn(false);
        when(roleMapper.toResponse(any(Role.class)))
                .thenReturn(new RoleResponse(2L, "MANAGER", "MANAGER", "Manager", false, null, null));

        RoleResponse response = userRoleService.assignRoleToUser(7L, 2L);

        assertThat(response.code()).isEqualTo("MANAGER");
    }

    @Test
    void assignInternalRoleToExternalEmailIsRejected() {
        stubOwner();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user("manager001@gmail.com")));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role("MANAGER")));

        assertThatThrownBy(() -> userRoleService.assignRoleToUser(7L, 2L))
                .isInstanceOf(InvalidEmailException.class)
                .hasMessageContaining("@rental.com");

        verify(userRoleRepository, never()).save(any());
    }

    @Test
    void assignTenantRoleToExternalEmailIsAllowed() {
        stubOwner();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user("john.doe@gmail.com")));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role("TENANT")));
        when(userRoleRepository.existsByUserIdAndRoleId(7L, 2L)).thenReturn(false);
        when(roleMapper.toResponse(any(Role.class)))
                .thenReturn(new RoleResponse(2L, "TENANT", "TENANT", "Tenant", false, null, null));

        RoleResponse response = userRoleService.assignRoleToUser(7L, 2L);

        assertThat(response.code()).isEqualTo("TENANT");
    }

    @Test
    void assignTenantRoleToRentalComEmailIsAlsoAllowed() {
        stubOwner();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user("john@rental.com")));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role("TENANT")));
        when(userRoleRepository.existsByUserIdAndRoleId(7L, 2L)).thenReturn(false);
        when(roleMapper.toResponse(any(Role.class)))
                .thenReturn(new RoleResponse(2L, "TENANT", "TENANT", "Tenant", false, null, null));

        RoleResponse response = userRoleService.assignRoleToUser(7L, 2L);

        assertThat(response.code()).isEqualTo("TENANT");
    }

    // ------------------------------------------------------------------
    // OWNER (without SUPER_ADMIN) cannot touch SUPER_ADMIN assignments
    // ------------------------------------------------------------------

    @Test
    void ownerCannotAssignSuperAdminRoleToUser() {
        stubOwner();
        when(securityUtils.isOwnerWithoutSuperAdmin()).thenReturn(true);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user("admin@rental.com")));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role("SUPER_ADMIN")));

        assertThatThrownBy(() -> userRoleService.assignRoleToUser(7L, 1L))
                .isInstanceOf(AccessDeniedException.class);
        verify(userRoleRepository, never()).save(any());
    }

    @Test
    void ownerCannotRemoveSuperAdminRoleFromUser() {
        stubOwner();
        when(securityUtils.isOwnerWithoutSuperAdmin()).thenReturn(true);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user("admin@rental.com")));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role("SUPER_ADMIN")));

        assertThatThrownBy(() -> userRoleService.removeRoleFromUser(7L, 1L))
                .isInstanceOf(AccessDeniedException.class);
        verify(userRoleRepository, never()).deleteByUserIdAndRoleId(any(Long.class), any(Long.class));
    }

    @Test
    void ownerUserRolesListFiltersOutSuperAdmin() {
        stubOwner();
        when(securityUtils.getCurrentUserId()).thenReturn(5L);
        when(securityUtils.isOwnerWithoutSuperAdmin()).thenReturn(true);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user("manager001@rental.com")));
        Role superAdmin = role("SUPER_ADMIN");
        Role manager = role("MANAGER");
        when(userRoleRepository.findByUserId(7L)).thenReturn(List.of(assignment(superAdmin), assignment(manager)));
        when(roleMapper.toResponse(any(Role.class)))
                .thenAnswer(inv -> {
                    Role r = inv.getArgument(0);
                    return new RoleResponse(r.getId(), r.getCode(), r.getCode(), r.getName(), false, null, null);
                });

        List<RoleResponse> result = userRoleService.getUserRoles(7L);

        assertThat(result).extracting(RoleResponse::code).containsExactly("MANAGER");
    }

    @Test
    void superAdminUserRolesListStillContainsSuperAdmin() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
        when(securityUtils.getCurrentUserId()).thenReturn(5L);
        when(securityUtils.isOwnerWithoutSuperAdmin()).thenReturn(false);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user("admin@rental.com")));
        Role superAdmin = role("SUPER_ADMIN");
        Role manager = role("MANAGER");
        when(userRoleRepository.findByUserId(7L)).thenReturn(List.of(assignment(superAdmin), assignment(manager)));
        when(roleMapper.toResponse(any(Role.class)))
                .thenAnswer(inv -> {
                    Role r = inv.getArgument(0);
                    return new RoleResponse(r.getId(), r.getCode(), r.getCode(), r.getName(), false, null, null);
                });

        List<RoleResponse> result = userRoleService.getUserRoles(7L);

        assertThat(result).extracting(RoleResponse::code)
                .containsExactly("SUPER_ADMIN", "MANAGER");
    }

    private com.example.spring_boot_project_api.model.UserRole assignment(Role role) {
        return com.example.spring_boot_project_api.model.UserRole.builder().role(role).build();
    }

    // ------------------------------------------------------------------
    // OWNER may only manage roles of users inside their ownership scope
    // ------------------------------------------------------------------

    @Test
    void ownerCannotAssignRoleToAnotherOwnersUser() {
        stubOwner();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user("tenant@rental.com")));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role("MANAGER")));

        doThrow(new AccessDeniedException("You do not have permission to access this resource"))
                .when(userService).requireUserManagementAccess(7L);

        assertThatThrownBy(() -> userRoleService.assignRoleToUser(7L, 2L))
                .isInstanceOf(AccessDeniedException.class);
        verify(userRoleRepository, never()).save(any());
    }

    @Test
    void ownerCannotRemoveRoleFromAnotherOwnersUser() {
        stubOwner();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user("tenant@rental.com")));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role("MANAGER")));

        doThrow(new AccessDeniedException("You do not have permission to access this resource"))
                .when(userService).requireUserManagementAccess(7L);

        assertThatThrownBy(() -> userRoleService.removeRoleFromUser(7L, 2L))
                .isInstanceOf(AccessDeniedException.class);
        verify(userRoleRepository, never()).deleteByUserIdAndRoleId(any(Long.class), any(Long.class));
    }

    @Test
    void ownerCannotAssignRoleToSuperAdminUser() {
        stubOwner();
        when(securityUtils.isOwnerWithoutSuperAdmin()).thenReturn(true);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user("admin@rental.com")));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role("MANAGER")));

        doThrow(new AccessDeniedException("You do not have permission to access this resource"))
                .when(userService).requireUserManagementAccess(7L);

        assertThatThrownBy(() -> userRoleService.assignRoleToUser(7L, 2L))
                .isInstanceOf(AccessDeniedException.class);
        verify(userRoleRepository, never()).save(any());
    }

    @Test
    void ownerCannotPromoteUnrelatedUserToManagerRole() {
        stubOwner();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user("target@rental.com")));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role("MANAGER")));

        doThrow(new AccessDeniedException("You do not have permission to access this resource"))
                .when(userService).requireUserManagementAccess(7L);

        assertThatThrownBy(() -> userRoleService.assignRoleToUser(7L, 2L))
                .isInstanceOf(AccessDeniedException.class);
        verify(userRoleRepository, never()).save(any());
    }

    @Test
    void ownerCannotPromoteUnrelatedUserToOwnerRole() {
        stubOwner();
        when(securityUtils.isOwnerWithoutSuperAdmin()).thenReturn(true);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user("target@rental.com")));
        when(roleRepository.findById(9L)).thenReturn(Optional.of(role("OWNER")));

        assertThatThrownBy(() -> userRoleService.assignRoleToUser(7L, 9L))
                .isInstanceOf(AccessDeniedException.class);
        verify(userRoleRepository, never()).save(any());
    }

    @Test
    void ownerCannotPromoteInScopeUserToOwnerRole() {
        stubOwner();
        when(securityUtils.isOwnerWithoutSuperAdmin()).thenReturn(true);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user("target@rental.com")));
        when(roleRepository.findById(9L)).thenReturn(Optional.of(role("OWNER")));

        assertThatThrownBy(() -> userRoleService.assignRoleToUser(7L, 9L))
                .isInstanceOf(AccessDeniedException.class);
        verify(userService, never()).requireUserManagementAccess(any(Long.class));
        verify(userRoleRepository, never()).save(any());
    }

    @Test
    void ownerCannotRemoveOwnerRoleFromInScopeUser() {
        stubOwner();
        when(securityUtils.isOwnerWithoutSuperAdmin()).thenReturn(true);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user("target@rental.com")));
        when(roleRepository.findById(9L)).thenReturn(Optional.of(role("OWNER")));

        assertThatThrownBy(() -> userRoleService.removeRoleFromUser(7L, 9L))
                .isInstanceOf(AccessDeniedException.class);
        verify(userRoleRepository, never()).deleteByUserIdAndRoleId(any(Long.class), any(Long.class));
    }

    @Test
    void ownerCanAssignManagerRoleToOwnTenantUser() {
        stubOwner();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user("tenant@rental.com")));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role("MANAGER")));
        when(userRoleRepository.existsByUserIdAndRoleId(7L, 2L)).thenReturn(false);
        when(roleMapper.toResponse(any(Role.class)))
                .thenReturn(new RoleResponse(2L, "MANAGER", "MANAGER", "Manager", false, null, null));

        RoleResponse response = userRoleService.assignRoleToUser(7L, 2L);

        assertThat(response.code()).isEqualTo("MANAGER");
        verify(userService).requireUserManagementAccess(7L);
        verify(userRoleRepository).save(any());
    }

    @Test
    void superAdminCanStillAssignRoleToAnyUser() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user("someone@rental.com")));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role("OWNER")));
        when(userRoleRepository.existsByUserIdAndRoleId(7L, 2L)).thenReturn(false);
        when(roleMapper.toResponse(any(Role.class)))
                .thenReturn(new RoleResponse(2L, "OWNER", "OWNER", "Owner", false, null, null));

        RoleResponse response = userRoleService.assignRoleToUser(7L, 2L);

        assertThat(response.code()).isEqualTo("OWNER");
        verify(userRoleRepository).save(any());
    }

    @Test
    void superAdminCanStillRemoveRoleFromAnyUser() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user("someone@rental.com")));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role("SUPER_ADMIN")));
        when(userRoleRepository.existsByUserIdAndRoleId(7L, 1L)).thenReturn(true);

        userRoleService.removeRoleFromUser(7L, 1L);

        verify(userRoleRepository).deleteByUserIdAndRoleId(7L, 1L);
    }

    @Test
    void tenantRoleCannotBeAssignedByNonStaffCaller() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> userRoleService.assignRoleToUser(7L, 2L))
                .isInstanceOf(AccessDeniedException.class);
        verify(userRepository, never()).findById(any(Long.class));
    }
}