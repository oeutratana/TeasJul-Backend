package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.UpdateTenantRequest;
import com.example.spring_boot_project_api.dto.response.TenantMeResponse;
import com.example.spring_boot_project_api.dto.response.TenantUtilityResponse;

public interface TenantPortalService {

    TenantMeResponse getMyTenantProfile();

    TenantMeResponse updateMyTenantProfile(UpdateTenantRequest request);

    List<TenantUtilityResponse> getMyUtilities();
}