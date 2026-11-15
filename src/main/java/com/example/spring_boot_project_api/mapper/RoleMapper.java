package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.CreateRoleRequest;
import com.example.spring_boot_project_api.dto.response.RoleResponse;
import com.example.spring_boot_project_api.model.Role;

@Component
public class RoleMapper {

    public RoleResponse toResponse(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getCode(),
                role.getDescription(),
                role.getIsSystemRole(),
                role.getCreatedAt(),
                role.getUpdatedAt());
    }

    public Role toEntity(CreateRoleRequest request) {
        return Role.builder()
                .name(request.name())
                .code(request.code())
                .description(request.description())
                .isSystemRole(request.isSystemRole() != null ? request.isSystemRole() : false)
                .build();
    }
}
