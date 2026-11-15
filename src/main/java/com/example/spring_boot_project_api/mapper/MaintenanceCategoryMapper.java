package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.CreateMaintenanceCategoryRequest;
import com.example.spring_boot_project_api.dto.response.MaintenanceCategoryResponse;
import com.example.spring_boot_project_api.model.MaintenanceCategory;
import com.example.spring_boot_project_api.model.MaintenanceCategoryStatus;

@Component
public class MaintenanceCategoryMapper {

    public MaintenanceCategoryResponse toResponse(MaintenanceCategory category) {
        return new MaintenanceCategoryResponse(
                category.getId(),
                category.getOwner().getId(),
                category.getName(),
                category.getCode(),
                category.getDescription(),
                category.getStatus(),
                category.getCreatedAt(),
                category.getUpdatedAt());
    }

    public MaintenanceCategory toEntity(CreateMaintenanceCategoryRequest request) {
        return MaintenanceCategory.builder()
                .name(request.name())
                .code(request.code())
                .description(request.description())
                .status(MaintenanceCategoryStatus.ACTIVE)
                .build();
    }
}
