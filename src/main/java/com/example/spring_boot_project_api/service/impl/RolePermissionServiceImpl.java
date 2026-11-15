package com.example.spring_boot_project_api.service.impl;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.ReplaceRolePermissionsRequest;
import com.example.spring_boot_project_api.dto.response.PermissionResponse;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.PermissionNotFoundException;
import com.example.spring_boot_project_api.exception.RoleNotFoundException;
import com.example.spring_boot_project_api.exception.RolePermissionNotFoundException;
import com.example.spring_boot_project_api.mapper.PermissionMapper;
import com.example.spring_boot_project_api.model.Permission;
import com.example.spring_boot_project_api.model.Role;
import com.example.spring_boot_project_api.model.RolePermission;
import com.example.spring_boot_project_api.model.RolePermissionId;
import com.example.spring_boot_project_api.repository.PermissionRepository;
import com.example.spring_boot_project_api.repository.RolePermissionRepository;
import com.example.spring_boot_project_api.repository.RoleRepository;
import com.example.spring_boot_project_api.service.RolePermissionService;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RolePermissionServiceImpl implements RolePermissionService {

    private final RolePermissionRepository rolePermissionRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> getRolePermissions(Long roleId) {
        Role role = getRoleOrThrow(roleId);
        ensureOwnerMayManage(role);

        return rolePermissionRepository.findByRoleId(roleId).stream()
                .map(assignment -> permissionMapper.toResponse(assignment.getPermission()))
                .toList();
    }

    @Override
    @Transactional
    public PermissionResponse assignPermissionToRole(Long roleId, Long permissionId) {
        requireOwner();
        Role role = getRoleOrThrow(roleId);
        ensureOwnerMayManage(role);
        Permission permission = getPermissionOrThrow(permissionId);

        if (rolePermissionRepository.existsByRoleIdAndPermissionId(roleId, permissionId)) {
            throw new DuplicateResourceException("Permission " + permission.getCode()
                    + " is already assigned to role " + role.getCode());
        }

        RolePermission assignment = RolePermission.builder()
                .id(new RolePermissionId(roleId, permissionId))
                .role(role)
                .permission(permission)
                .build();

        return permissionMapper.toResponse(
                rolePermissionRepository.saveAndFlush(assignment).getPermission());
    }

    @Override
    @Transactional
    public void removePermissionFromRole(Long roleId, Long permissionId) {
        requireOwner();
        Role role = getRoleOrThrow(roleId);
        ensureOwnerMayManage(role);
        ensureNotOwnerRole(role);

        if (!rolePermissionRepository.existsByRoleIdAndPermissionId(roleId, permissionId)) {
            throw RolePermissionNotFoundException.forIds(roleId, permissionId);
        }

        rolePermissionRepository.deleteByRoleIdAndPermissionId(roleId, permissionId);
    }

    @Override
    @Transactional
    public List<PermissionResponse> replaceRolePermissions(Long roleId,
            ReplaceRolePermissionsRequest request) {
        requireOwner();
        Role role = getRoleOrThrow(roleId);
        ensureOwnerMayManage(role);
        ensureNotOwnerRole(role);

        Set<Long> permissionIds = new LinkedHashSet<>(request.permissionIds());

        Map<Long, Permission> permissionsById = permissionRepository.findAllById(permissionIds).stream()
                .collect(Collectors.toMap(Permission::getId, Function.identity()));

        permissionIds.stream()
                .filter(id -> !permissionsById.containsKey(id))
                .findFirst()
                .ifPresent(id -> {
                    throw PermissionNotFoundException.forId(id);
                });

        rolePermissionRepository.deleteByRoleId(roleId);

        List<RolePermission> assignments = permissionIds.stream()
                .map(permissionId -> RolePermission.builder()
                        .id(new RolePermissionId(roleId, permissionId))
                        .role(role)
                        .permission(permissionsById.get(permissionId))
                        .build())
                .toList();

        rolePermissionRepository.saveAll(assignments);

        return assignments.stream()
                .map(assignment -> permissionMapper.toResponse(assignment.getPermission()))
                .toList();
    }

    private void requireOwner() {
        if (!securityUtils.hasRole(SecurityUtils.ROLE_OWNER)
                && !securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)) {
            throw new AccessDeniedException("You do not have permission to access this resource");
        }
    }

    /**
     * The OWNER role always keeps every permission: it cannot be downgraded through
     * the permission management APIs (enforced here in addition to the UI).
     */
    private void ensureNotOwnerRole(Role role) {
        if (SecurityUtils.ROLE_OWNER.equalsIgnoreCase(role.getCode())) {
            throw new AccessDeniedException(
                    "The Owner role always retains all permissions and cannot be modified");
        }
    }

    /**
     * An OWNER (who is not also a SUPER_ADMIN) must never read or manage the
     * SUPER_ADMIN role's permissions. SUPER_ADMIN users keep full access.
     */
    private void ensureOwnerMayManage(Role role) {
        if (securityUtils.isOwnerWithoutSuperAdmin()
                && SecurityUtils.ROLE_SUPER_ADMIN.equalsIgnoreCase(role.getCode())) {
            throw new AccessDeniedException("You do not have permission to access this resource");
        }
    }

    private Role getRoleOrThrow(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> RoleNotFoundException.forId(roleId));
    }

    private Permission getPermissionOrThrow(Long permissionId) {
        return permissionRepository.findById(permissionId)
                .orElseThrow(() -> PermissionNotFoundException.forId(permissionId));
    }
}
