package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.CreateSystemDefaultRequest;
import com.example.spring_boot_project_api.dto.request.UpdateSystemDefaultRequest;
import com.example.spring_boot_project_api.dto.response.SystemDefaultResponse;
import com.example.spring_boot_project_api.dto.response.SystemDefaultViewResponse;
import com.example.spring_boot_project_api.enums.SystemDefaultKind;
import com.example.spring_boot_project_api.enums.SystemDefaultStatus;

public interface SystemDefaultService {

    SystemDefaultResponse createDefault(CreateSystemDefaultRequest request);

    List<SystemDefaultResponse> listDefaults(SystemDefaultKind kind, SystemDefaultStatus status,
            String search);

    SystemDefaultResponse updateDefault(Long id, UpdateSystemDefaultRequest request);

    void updateDefaultStatus(Long id, SystemDefaultStatus status);

    List<SystemDefaultViewResponse> listDefaultsForOwner(SystemDefaultKind kind);

    SystemDefaultViewResponse enableForOwner(Long id);

    SystemDefaultViewResponse disableForOwner(Long id);
}