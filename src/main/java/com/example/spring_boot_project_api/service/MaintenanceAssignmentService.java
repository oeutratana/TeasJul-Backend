package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.CreateMaintenanceAssignmentRequest;
import com.example.spring_boot_project_api.dto.response.MaintenanceAssignmentResponse;

public interface MaintenanceAssignmentService {

    MaintenanceAssignmentResponse createAssignment(Long requestId, CreateMaintenanceAssignmentRequest request);

    List<MaintenanceAssignmentResponse> getAssignments(Long requestId);

    void completeAssignment(Long requestId, Long assignmentId);
}
