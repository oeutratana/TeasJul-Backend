package com.example.spring_boot_project_api.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.spring_boot_project_api.dto.request.CreatePermissionRequest;
import com.example.spring_boot_project_api.dto.request.UpdatePermissionRequest;
import com.example.spring_boot_project_api.dto.response.PermissionResponse;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.PermissionNotFoundException;
import com.example.spring_boot_project_api.mapper.PermissionMapper;
import com.example.spring_boot_project_api.model.Permission;
import com.example.spring_boot_project_api.repository.PermissionRepository;
import com.example.spring_boot_project_api.service.PermissionService;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public PermissionResponse createPermission(CreatePermissionRequest request) {
        requireOwner();
        if (permissionRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException("Permission code already exists: " + request.code());
        }

        return permissionMapper.toResponse(permissionRepository.save(permissionMapper.toEntity(request)));
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionResponse getPermissionById(Long id) {
        return permissionMapper.toResponse(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissions(String module) {
        List<Permission> permissions = StringUtils.hasText(module)
                ? permissionRepository.findByModuleIgnoreCase(module)
                : permissionRepository.findAll();

        return permissions.stream()
                .map(permissionMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public PermissionResponse updatePermission(Long id, UpdatePermissionRequest request) {
        requireOwner();
        Permission permission = getEntityById(id);

        if (request.code() != null && permissionRepository.existsByCodeAndIdNot(request.code(), id)) {
            throw new DuplicateResourceException("Permission code already exists: " + request.code());
        }

        if (request.name() != null) {
            permission.setName(request.name());
        }
        if (request.code() != null) {
            permission.setCode(request.code());
        }
        if (request.module() != null) {
            permission.setModule(request.module());
        }
        if (request.description() != null) {
            permission.setDescription(request.description());
        }

        return permissionMapper.toResponse(permissionRepository.save(permission));
    }

    @Override
    @Transactional
    public void deletePermission(Long id) {
        requireOwner();
        permissionRepository.delete(getEntityById(id));
    }

    private void requireOwner() {
        if (!securityUtils.hasRole(SecurityUtils.ROLE_OWNER)
                && !securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You do not have permission to access this resource");
        }
    }

    private Permission getEntityById(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> PermissionNotFoundException.forId(id));
    }
}
