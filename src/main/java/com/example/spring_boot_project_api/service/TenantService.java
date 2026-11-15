package com.example.spring_boot_project_api.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.example.spring_boot_project_api.dto.request.CreateTenantRequest;
import com.example.spring_boot_project_api.dto.request.UpdateTenantRequest;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.TenantResponse;
import com.example.spring_boot_project_api.dto.response.TenantSummaryResponse;
import com.example.spring_boot_project_api.model.TenantStatus;

public interface TenantService {

    TenantResponse createTenant(CreateTenantRequest request);

    PageResponse<TenantResponse> getMyTenants(String search, TenantStatus status, Pageable pageable);

    TenantSummaryResponse getMyTenantsSummary();

    TenantResponse getTenantById(Long id);

    TenantResponse updateTenant(Long id, UpdateTenantRequest request);

    void deactivateTenant(Long id);
}
