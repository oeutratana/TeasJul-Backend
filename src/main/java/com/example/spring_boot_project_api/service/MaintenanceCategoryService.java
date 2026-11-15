package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.CreateMaintenanceCategoryRequest;
import com.example.spring_boot_project_api.dto.request.UpdateMaintenanceCategoryRequest;
import com.example.spring_boot_project_api.dto.response.MaintenanceCategoryResponse;

public interface MaintenanceCategoryService {

    MaintenanceCategoryResponse createCategory(CreateMaintenanceCategoryRequest request);

    List<MaintenanceCategoryResponse> getMyCategories();

    MaintenanceCategoryResponse getCategoryById(Long id);

    MaintenanceCategoryResponse updateCategory(Long id, UpdateMaintenanceCategoryRequest request);

    void deactivateCategory(Long id);
}
