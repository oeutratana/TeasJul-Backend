package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.CreatePermissionRequest;
import com.example.spring_boot_project_api.dto.response.PermissionResponse;
import com.example.spring_boot_project_api.model.Permission;

@Component
public class PermissionMapper {

    public PermissionResponse toResponse(Permission permission) {
        return new PermissionResponse(
                permission.getId(),
                permission.getName(),
                permission.getCode(),
                permission.getModule(),
                permission.getDescription(),
                permission.getCreatedAt());
    }

    public Permission toEntity(CreatePermissionRequest request) {
        return Permission.builder()
                .name(request.name())
                .code(request.code())
                .module(request.module())
                .description(request.description())
                .build();
    }
}
