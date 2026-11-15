package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.CreateMaintenanceRequestRequest;
import com.example.spring_boot_project_api.dto.response.MaintenanceRequestResponse;
import com.example.spring_boot_project_api.model.MaintenanceRequest;

@Component
public class MaintenanceRequestMapper {

    public MaintenanceRequestResponse toResponse(MaintenanceRequest request) {
        return new MaintenanceRequestResponse(
                request.getId(),
                request.getOwner().getId(),
                request.getRequestNo(),
                request.getHouse().getId(),
                request.getRoom().getId(),
                request.getTenant() != null ? request.getTenant().getId() : null,
                request.getCategory() != null ? request.getCategory().getId() : null,
                request.getTitle(),
                request.getDescription(),
                request.getPriority(),
                request.getStatus(),
                request.getReportedAt(),
                request.getResolvedAt(),
                request.getCreatedAt(),
                request.getUpdatedAt());
    }

    public MaintenanceRequest toEntity(CreateMaintenanceRequestRequest request) {
        return MaintenanceRequest.builder()
                .title(request.title())
                .description(request.description())
                .priority(request.priority())
                .build();
    }
}
