package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.CreateMaintenanceAssignmentRequest;
import com.example.spring_boot_project_api.dto.response.MaintenanceAssignmentResponse;
import com.example.spring_boot_project_api.model.MaintenanceAssignment;

@Component
public class MaintenanceAssignmentMapper {

    public MaintenanceAssignmentResponse toResponse(MaintenanceAssignment assignment) {
        return new MaintenanceAssignmentResponse(
                assignment.getId(),
                assignment.getRequest().getId(),
                assignment.getAssignedTo().getId(),
                assignment.getAssignedAt(),
                assignment.getCompletedAt(),
                assignment.getNotes());
    }

    public MaintenanceAssignment toEntity(CreateMaintenanceAssignmentRequest request) {
        return MaintenanceAssignment.builder()
                .notes(request.notes())
                .build();
    }
}
