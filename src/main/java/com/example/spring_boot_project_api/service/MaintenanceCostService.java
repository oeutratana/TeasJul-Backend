package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.CreateMaintenanceCostRequest;
import com.example.spring_boot_project_api.dto.request.UpdateMaintenanceCostRequest;
import com.example.spring_boot_project_api.dto.response.MaintenanceCostResponse;

public interface MaintenanceCostService {

    MaintenanceCostResponse createCost(Long requestId, CreateMaintenanceCostRequest request);

    List<MaintenanceCostResponse> getCosts(Long requestId);

    MaintenanceCostResponse updateCost(Long requestId, Long costId, UpdateMaintenanceCostRequest request);

    void deleteCost(Long requestId, Long costId);
}
