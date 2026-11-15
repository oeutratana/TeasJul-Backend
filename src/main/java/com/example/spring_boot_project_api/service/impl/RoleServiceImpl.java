package com.example.spring_boot_project_api.service.impl;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.CreateRoleRequest;
import com.example.spring_boot_project_api.dto.request.UpdateRoleRequest;
import com.example.spring_boot_project_api.dto.response.RoleResponse;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.RoleNotFoundException;
import com.example.spring_boot_project_api.mapper.RoleMapper;
import com.example.spring_boot_project_api.model.Role;
import com.example.spring_boot_project_api.repository.RoleRepository;
import com.example.spring_boot_project_api.service.RoleService;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        requireOwner();
        if (roleRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException("Role code already exists: " + request.code());
        }
        if (roleRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Role name already exists: " + request.name());
        }

        return roleMapper.toResponse(roleRepository.save(roleMapper.toEntity(request)));
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getRoleById(Long id) {
        Role role = getEntityById(id);
        ensureOwnerMayManage(role);
        return roleMapper.toResponse(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        boolean restrictSuperAdmin = securityUtils.isOwnerWithoutSuperAdmin();
        return roleRepository.findAll().stream()
                .filter(role -> !(restrictSuperAdmin && isSuperAdminRole(role.getCode())))
                .map(roleMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public RoleResponse updateRole(Long id, UpdateRoleRequest request) {
        requireOwner();
        Role role = getEntityById(id);
        ensureOwnerMayManage(role);

        if (request.code() != null && roleRepository.existsByCodeAndIdNot(request.code(), id)) {
            throw new DuplicateResourceException("Role code already exists: " + request.code());
        }
        if (request.name() != null && roleRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new DuplicateResourceException("Role name already exists: " + request.name());
        }

        if (request.name() != null) {
            role.setName(request.name());
        }
        if (request.code() != null) {
            role.setCode(request.code());
        }
        if (request.description() != null) {
            role.setDescription(request.description());
        }

        return roleMapper.toResponse(roleRepository.save(role));
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        requireOwner();
        Role role = getEntityById(id);
        ensureOwnerMayManage(role);

        if (Boolean.TRUE.equals(role.getIsSystemRole())) {
            throw new AccessDeniedException("System roles cannot be deleted");
        }

        roleRepository.delete(role);
    }

    private void requireOwner() {
        if (!securityUtils.hasRole(SecurityUtils.ROLE_OWNER)
                && !securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)) {
            throw new AccessDeniedException("You do not have permission to access this resource");
        }
    }

    /**
     * An OWNER (who is not also a SUPER_ADMIN) must never access or manage the
     * SUPER_ADMIN role. SUPER_ADMIN users keep full access.
     */
    private void ensureOwnerMayManage(Role role) {
        if (securityUtils.isOwnerWithoutSuperAdmin() && isSuperAdminRole(role.getCode())) {
            throw new AccessDeniedException("You do not have permission to access this resource");
        }
    }

    private boolean isSuperAdminRole(String code) {
        return SecurityUtils.ROLE_SUPER_ADMIN.equalsIgnoreCase(code);
    }

    private Role getEntityById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> RoleNotFoundException.forId(id));
    }
}
