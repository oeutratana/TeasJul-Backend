package com.example.spring_boot_project_api.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserStatus;
import com.example.spring_boot_project_api.repository.RolePermissionRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.UserRoleRepository;

@ExtendWith(MockitoExtension.class)
class SecurityUtilsTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @InjectMocks
    private SecurityUtils securityUtils;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(10L)
                .username("managerA")
                .email("managerA@example.com")
                .passwordHash("h")
                .firstName("Manager")
                .lastName("A")
                .status(UserStatus.ACTIVE)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void hasPermissionAlwaysGrantsOwnerWithoutQueryingRolePermissions() {
        when(userRoleRepository.existsByUserIdAndRoleCode(10L, SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);
        when(userRoleRepository.existsByUserIdAndRoleCode(10L, SecurityUtils.ROLE_OWNER)).thenReturn(true);

        boolean result = securityUtils.hasPermission(10L, PermissionConstants.HOUSE_MANAGE);

        assertThat(result).isTrue();
        verify(rolePermissionRepository, never())
                .existsByRoleIdInAndPermission_Code(anyCollection(), anyString());
    }

    @Test
    void hasPermissionAlwaysGrantsSuperAdminWithoutQueryingRolePermissions() {
        when(userRoleRepository.existsByUserIdAndRoleCode(10L, SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);

        boolean result = securityUtils.hasPermission(10L, PermissionConstants.HOUSE_MANAGE);

        assertThat(result).isTrue();
        verify(rolePermissionRepository, never())
                .existsByRoleIdInAndPermission_Code(anyCollection(), anyString());
    }

    @Test
    void hasPermissionReturnsFalseWhenUserHasNoRoles() {
        denySuperAdminRole();
        when(userRoleRepository.existsByUserIdAndRoleCode(10L, SecurityUtils.ROLE_OWNER)).thenReturn(false);
        when(userRoleRepository.findRoleIdsByUserId(10L)).thenReturn(List.of());

        boolean result = securityUtils.hasPermission(10L, PermissionConstants.HOUSE_MANAGE);

        assertThat(result).isFalse();
        verify(rolePermissionRepository, never())
                .existsByRoleIdInAndPermission_Code(anyCollection(), anyString());
    }

    @Test
    void hasPermissionChecksRolePermissionsForNonOwner() {
        denySuperAdminRole();
        when(userRoleRepository.existsByUserIdAndRoleCode(10L, SecurityUtils.ROLE_OWNER)).thenReturn(false);
        when(userRoleRepository.findRoleIdsByUserId(10L)).thenReturn(List.of(2L, 3L));
        when(rolePermissionRepository.existsByRoleIdInAndPermission_Code(List.of(2L, 3L), "PAYMENT_VIEW"))
                .thenReturn(true);

        boolean result = securityUtils.hasPermission(10L, "PAYMENT_VIEW");

        assertThat(result).isTrue();
    }

    @Test
    void hasPermissionReturnsFalseWhenRoleHasNotBeenGrantedThePermission() {
        denySuperAdminRole();
        when(userRoleRepository.existsByUserIdAndRoleCode(10L, SecurityUtils.ROLE_OWNER)).thenReturn(false);
        when(userRoleRepository.findRoleIdsByUserId(10L)).thenReturn(List.of(2L));
        when(rolePermissionRepository.existsByRoleIdInAndPermission_Code(List.of(2L), "EXPENSE_MANAGE"))
                .thenReturn(false);

        boolean result = securityUtils.hasPermission(10L, "EXPENSE_MANAGE");

        assertThat(result).isFalse();
    }

    @Test
    void requirePermissionDoesNotThrowWhenCurrentUserHasPermission() {
        authenticateAs(user);
        denySuperAdminRole();
        when(userRoleRepository.existsByUserIdAndRoleCode(10L, SecurityUtils.ROLE_OWNER)).thenReturn(false);
        when(userRoleRepository.findRoleIdsByUserId(10L)).thenReturn(List.of(3L));
        when(rolePermissionRepository.existsByRoleIdInAndPermission_Code(List.of(3L), "MAINTENANCE_VIEW"))
                .thenReturn(true);

        securityUtils.requirePermission("MAINTENANCE_VIEW");
    }

    @Test
    void requirePermissionThrowsForMissingPermission() {
        authenticateAs(user);
        denySuperAdminRole();
        when(userRoleRepository.existsByUserIdAndRoleCode(10L, SecurityUtils.ROLE_OWNER)).thenReturn(false);
        when(userRoleRepository.findRoleIdsByUserId(10L)).thenReturn(List.of(3L));
        when(rolePermissionRepository.existsByRoleIdInAndPermission_Code(anyCollection(), anyString()))
                .thenReturn(false);

        assertThatThrownBy(() -> securityUtils.requirePermission("SETTING_UPDATE"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void requirePermissionThrowsForCurrentUserWithoutRoles() {
        authenticateAs(user);
        denySuperAdminRole();
        when(userRoleRepository.existsByUserIdAndRoleCode(10L, SecurityUtils.ROLE_OWNER)).thenReturn(false);
        when(userRoleRepository.findRoleIdsByUserId(10L)).thenReturn(List.of());

        assertThatThrownBy(() -> securityUtils.requirePermission("SETTING_UPDATE"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void isOwnerWithoutSuperAdminTrueWhenOwnerOnly() {
        authenticateAs(user);
        when(userRoleRepository.existsByUserIdAndRoleCode(10L, SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);
        when(userRoleRepository.existsByUserIdAndRoleCode(10L, SecurityUtils.ROLE_OWNER)).thenReturn(true);

        assertThat(securityUtils.isOwnerWithoutSuperAdmin()).isTrue();
    }

    @Test
    void isOwnerWithoutSuperAdminFalseWhenAlsoSuperAdmin() {
        authenticateAs(user);
        when(userRoleRepository.existsByUserIdAndRoleCode(10L, SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
        when(userRoleRepository.existsByUserIdAndRoleCode(10L, SecurityUtils.ROLE_OWNER)).thenReturn(true);

        assertThat(securityUtils.isOwnerWithoutSuperAdmin()).isFalse();
    }

    @Test
    void isOwnerWithoutSuperAdminFalseWhenNotOwner() {
        authenticateAs(user);
        when(userRoleRepository.existsByUserIdAndRoleCode(10L, SecurityUtils.ROLE_OWNER)).thenReturn(false);

        assertThat(securityUtils.isOwnerWithoutSuperAdmin()).isFalse();
    }

    private void denySuperAdminRole() {
        when(userRoleRepository.existsByUserIdAndRoleCode(10L, SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(false);
    }

    private void authenticateAs(User currentUser) {
        UserDetails details = org.springframework.security.core.userdetails.User.builder()
                .username(currentUser.getUsername())
                .password(currentUser.getPasswordHash())
                .roles("USER")
                .build();
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(details, "n/a"));
        when(userRepository.findByUsername(currentUser.getUsername())).thenReturn(Optional.of(currentUser));
    }
}