package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.ChangeMaintenanceRequestStatusRequest;
import com.example.spring_boot_project_api.dto.request.CreateMaintenanceRequestRequest;
import com.example.spring_boot_project_api.dto.request.CreateTenantMaintenanceRequestRequest;
import com.example.spring_boot_project_api.dto.request.UpdateMaintenanceRequestRequest;
import com.example.spring_boot_project_api.dto.response.MaintenanceRequestResponse;

public interface MaintenanceRequestService {

    MaintenanceRequestResponse createRequest(CreateMaintenanceRequestRequest request);

    List<MaintenanceRequestResponse> getMyRequests();

    List<MaintenanceRequestResponse> getMyRequestsAsTenant();

    MaintenanceRequestResponse createMyRequest(CreateTenantMaintenanceRequestRequest request);

    MaintenanceRequestResponse getRequestById(Long id);

    MaintenanceRequestResponse updateRequest(Long id, UpdateMaintenanceRequestRequest request);

    void changeStatus(Long id, ChangeMaintenanceRequestStatusRequest request);
}
