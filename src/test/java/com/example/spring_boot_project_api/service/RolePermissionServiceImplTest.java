package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

import com.example.spring_boot_project_api.dto.request.ReplaceRolePermissionsRequest;
import com.example.spring_boot_project_api.dto.response.PermissionResponse;
import com.example.spring_boot_project_api.mapper.PermissionMapper;
import com.example.spring_boot_project_api.model.Permission;
import com.example.spring_boot_project_api.model.Role;
import com.example.spring_boot_project_api.repository.PermissionRepository;
import com.example.spring_boot_project_api.repository.RolePermissionRepository;
import com.example.spring_boot_project_api.repository.RoleRepository;
import com.example.spring_boot_project_api.service.impl.RolePermissionServiceImpl;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
class RolePermissionServiceImplTest {

    @Mock
    private RolePermissionRepository rolePermissionRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PermissionRepository permissionRepository;
    @Mock
    private PermissionMapper permissionMapper;
    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private RolePermissionServiceImpl rolePermissionService;

    private Role role(Long id, String code) {
        return Role.builder().id(id).name(code).code(code).isSystemRole(true).build();
    }

    private Permission permission(Long id, String code) {
        return Permission.builder().id(id).name("Some permission").code(code).module("MODULE").build();
    }

    // ------------------------------------------------------------------
    // Owner role lock
    // ------------------------------------------------------------------

    @Test
    void removePermissionFromOwnerRoleIsBlocked() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(true);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role(1L, "OWNER")));

        assertThatThrownBy(() -> rolePermissionService.removePermissionFromRole(1L, 10L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Owner role always retains all permissions");
        verify(rolePermissionRepository, never()).deleteByRoleIdAndPermissionId(anyLong(), anyLong());
    }

    @Test
    void replacePermissionsOnOwnerRoleIsBlocked() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(true);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role(1L, "OWNER")));

        assertThatThrownBy(() -> rolePermissionService.replaceRolePermissions(1L,
                new ReplaceRolePermissionsRequest(List.of(10L))))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Owner role always retains all permissions");
        verify(rolePermissionRepository, never()).deleteByRoleId(anyLong());
    }

    @Test
    void removePermissionFromOwnerRoleIsBlockedRegardlessOfRoleCodeCase() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(true);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role(1L, "owner")));

        assertThatThrownBy(() -> rolePermissionService.removePermissionFromRole(1L, 10L))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ------------------------------------------------------------------
    // Manager role is modifiable
    // ------------------------------------------------------------------

    @Test
    void removePermissionFromManagerRoleRemovesAssignment() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(true);
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role(2L, "MANAGER")));
        when(rolePermissionRepository.existsByRoleIdAndPermissionId(2L, 10L)).thenReturn(true);

        rolePermissionService.removePermissionFromRole(2L, 10L);

        verify(rolePermissionRepository).deleteByRoleIdAndPermissionId(2L, 10L);
    }

    @Test
    void removePermissionFromManagerRoleThrowsWhenNotAssigned() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(true);
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role(2L, "MANAGER")));
        when(rolePermissionRepository.existsByRoleIdAndPermissionId(2L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> rolePermissionService.removePermissionFromRole(2L, 10L))
                .isInstanceOf(Exception.class);
        verify(rolePermissionRepository, never()).deleteByRoleIdAndPermissionId(anyLong(), anyLong());
    }

    @Test
    void replacePermissionsReplacesManagerRoleAssignments() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(true);
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role(2L, "MANAGER")));
        Permission p1 = permission(10L, "PAYMENT_VIEW");
        Permission p2 = permission(11L, "PAYMENT_CREATE");
        when(permissionRepository.findAllById(any())).thenReturn(List.of(p1, p2));
        when(permissionMapper.toResponse(any(Permission.class)))
                .thenAnswer(inv -> {
                    Permission p = inv.getArgument(0);
                    return new PermissionResponse(p.getId(), p.getName(), p.getCode(), p.getModule(),
                            p.getDescription(), p.getCreatedAt());
                });

        List<PermissionResponse> result = rolePermissionService.replaceRolePermissions(2L,
                new ReplaceRolePermissionsRequest(List.of(10L, 11L)));

        assertThat(result).extracting(PermissionResponse::code)
                .containsExactly("PAYMENT_VIEW", "PAYMENT_CREATE");
        verify(rolePermissionRepository).deleteByRoleId(2L);
        verify(rolePermissionRepository).saveAll(any());
    }

    @Test
    void replacePermissionsThrowsWhenAPermissionDoesNotExist() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(true);
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role(2L, "MANAGER")));
        when(permissionRepository.findAllById(any())).thenReturn(List.of(permission(10L, "PAYMENT_VIEW")));

        assertThatThrownBy(() -> rolePermissionService.replaceRolePermissions(2L,
                new ReplaceRolePermissionsRequest(List.of(10L, 99L))))
                .isInstanceOf(Exception.class);
        verify(rolePermissionRepository, never()).deleteByRoleId(anyLong());
    }

    // ------------------------------------------------------------------
    // OWNER (without SUPER_ADMIN) cannot read/manage SUPER_ADMIN permissions
    // ------------------------------------------------------------------

    @Test
    void getRolePermissionsOnSuperAdminIsBlockedForOwner() {
        when(securityUtils.isOwnerWithoutSuperAdmin()).thenReturn(true);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role(1L, "SUPER_ADMIN")));

        assertThatThrownBy(() -> rolePermissionService.getRolePermissions(1L))
                .isInstanceOf(AccessDeniedException.class);
        verify(rolePermissionRepository, never()).findByRoleId(anyLong());
    }

    @Test
    void assignPermissionToSuperAdminIsBlockedForOwner() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(true);
        when(securityUtils.isOwnerWithoutSuperAdmin()).thenReturn(true);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role(1L, "SUPER_ADMIN")));

        assertThatThrownBy(() -> rolePermissionService.assignPermissionToRole(1L, 10L))
                .isInstanceOf(AccessDeniedException.class);
        verify(rolePermissionRepository, never()).saveAndFlush(any());
    }

    @Test
    void removePermissionFromSuperAdminIsBlockedForOwner() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(true);
        when(securityUtils.isOwnerWithoutSuperAdmin()).thenReturn(true);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role(1L, "SUPER_ADMIN")));

        assertThatThrownBy(() -> rolePermissionService.removePermissionFromRole(1L, 10L))
                .isInstanceOf(AccessDeniedException.class);
        verify(rolePermissionRepository, never()).deleteByRoleIdAndPermissionId(anyLong(), anyLong());
    }

    @Test
    void replacePermissionsOnSuperAdminIsBlockedForOwner() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(true);
        when(securityUtils.isOwnerWithoutSuperAdmin()).thenReturn(true);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role(1L, "SUPER_ADMIN")));

        assertThatThrownBy(() -> rolePermissionService.replaceRolePermissions(1L,
                new ReplaceRolePermissionsRequest(List.of(10L))))
                .isInstanceOf(AccessDeniedException.class);
        verify(rolePermissionRepository, never()).deleteByRoleId(anyLong());
    }

    @Test
    void superAdminCanStillManageSuperAdminPermissions() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);
        when(securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)).thenReturn(true);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role(1L, "SUPER_ADMIN")));
        Permission p1 = permission(10L, "LISTING_APPROVE");
        when(permissionRepository.findAllById(any())).thenReturn(List.of(p1));
        when(permissionMapper.toResponse(any(Permission.class)))
                .thenAnswer(inv -> {
                    Permission p = inv.getArgument(0);
                    return new PermissionResponse(p.getId(), p.getName(), p.getCode(), p.getModule(),
                            p.getDescription(), p.getCreatedAt());
                });

        List<PermissionResponse> result = rolePermissionService.replaceRolePermissions(1L,
                new ReplaceRolePermissionsRequest(List.of(10L)));

        assertThat(result).extracting(PermissionResponse::code)
                .containsExactly("LISTING_APPROVE");
        verify(rolePermissionRepository).deleteByRoleId(1L);
        verify(rolePermissionRepository).saveAll(any());
    }
}