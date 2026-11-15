package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import com.example.spring_boot_project_api.dto.request.UpdateRoleRequest;
import com.example.spring_boot_project_api.dto.response.RoleResponse;
import com.example.spring_boot_project_api.mapper.RoleMapper;
import com.example.spring_boot_project_api.model.Role;
import com.example.spring_boot_project_api.repository.RoleRepository;
import com.example.spring_boot_project_api.service.impl.RoleServiceImpl;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private RoleServiceImpl roleService;

    private Role role(Long id, String code) {
        return Role.builder().id(id).name(code).code(code).isSystemRole(true).build();
    }

    private void stubMapper() {
        when(roleMapper.toResponse(any(Role.class)))
                .thenAnswer(inv -> {
                    Role r = inv.getArgument(0);
                    return new RoleResponse(r.getId(), r.getName(), r.getCode(), r.getDescription(),
                            r.getIsSystemRole(), r.getCreatedAt(), r.getUpdatedAt());
                });
    }

    /** Current user is an OWNER who is not a SUPER_ADMIN. */
    private void stubOwnerOnly() {
        when(securityUtils.isOwnerWithoutSuperAdmin()).thenReturn(true);
    }

    /** Current user is an OWNER (not SUPER_ADMIN) holding write access. */
    private void stubOwnerWriteAccess() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(true);
        when(securityUtils.isOwnerWithoutSuperAdmin()).thenReturn(true);
    }

    // ------------------------------------------------------------------
    // Role list filtering
    // ------------------------------------------------------------------

    @Test
    void ownerRoleListDoesNotContainSuperAdmin() {
        stubOwnerOnly();
        when(roleRepository.findAll())
                .thenReturn(List.of(role(1L, "SUPER_ADMIN"), role(2L, "OWNER"),
                        role(3L, "MANAGER"), role(4L, "TENANT")));
        stubMapper();

        List<RoleResponse> result = roleService.getAllRoles();

        assertThat(result).extracting(RoleResponse::code)
                .containsExactly("OWNER", "MANAGER", "TENANT");
    }

    @Test
    void superAdminRoleListStillContainsSuperAdmin() {
        when(securityUtils.isOwnerWithoutSuperAdmin()).thenReturn(false);
        when(roleRepository.findAll())
                .thenReturn(List.of(role(1L, "SUPER_ADMIN"), role(2L, "OWNER"),
                        role(3L, "MANAGER"), role(4L, "TENANT")));
        stubMapper();

        List<RoleResponse> result = roleService.getAllRoles();

        assertThat(result).extracting(RoleResponse::code)
                .containsExactly("SUPER_ADMIN", "OWNER", "MANAGER", "TENANT");
    }

    // ------------------------------------------------------------------
    // OWNER cannot access/manage SUPER_ADMIN
    // ------------------------------------------------------------------

    @Test
    void ownerCannotGetSuperAdminRoleDetails() {
        stubOwnerOnly();
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role(1L, "SUPER_ADMIN")));

        assertThatThrownBy(() -> roleService.getRoleById(1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void ownerCannotUpdateSuperAdminRole() {
        stubOwnerWriteAccess();
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role(1L, "SUPER_ADMIN")));

        assertThatThrownBy(() -> roleService.updateRole(1L,
                new UpdateRoleRequest("Super Admin", "SUPER_ADMIN", "changed")))
                .isInstanceOf(AccessDeniedException.class);
        verify(roleRepository, never()).save(any());
    }

    @Test
    void ownerCannotDeleteSuperAdminRole() {
        stubOwnerWriteAccess();
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role(1L, "SUPER_ADMIN")));

        assertThatThrownBy(() -> roleService.deleteRole(1L))
                .isInstanceOf(AccessDeniedException.class);
        verify(roleRepository, never()).delete(any());
    }

    // ------------------------------------------------------------------
    // OWNER can still manage normal business roles
    // ------------------------------------------------------------------

    @Test
    void ownerCanStillGetManagerRoleDetails() {
        when(securityUtils.isOwnerWithoutSuperAdmin()).thenReturn(true);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role(1L, "MANAGER")));
        stubMapper();

        RoleResponse result = roleService.getRoleById(1L);

        assertThat(result.code()).isEqualTo("MANAGER");
    }

    @Test
    void ownerCanStillUpdateManagerRole() {
        stubOwnerWriteAccess();
        Role manager = role(1L, "MANAGER");
        when(roleRepository.findById(1L)).thenReturn(Optional.of(manager));
        when(roleMapper.toResponse(any(Role.class)))
                .thenReturn(new RoleResponse(1L, "New Manager", "MANAGER", "d", false, null, null));
        when(roleRepository.save(manager)).thenReturn(manager);

        RoleResponse result = roleService.updateRole(1L,
                new UpdateRoleRequest("New Manager", null, null));

        assertThat(result.name()).isEqualTo("New Manager");
        verify(roleRepository).save(manager);
    }
}