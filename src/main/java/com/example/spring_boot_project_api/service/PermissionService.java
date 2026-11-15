package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.CreatePermissionRequest;
import com.example.spring_boot_project_api.dto.request.UpdatePermissionRequest;
import com.example.spring_boot_project_api.dto.response.PermissionResponse;

public interface PermissionService {

    PermissionResponse createPermission(CreatePermissionRequest request);

    PermissionResponse getPermissionById(Long id);

    List<PermissionResponse> getAllPermissions(String module);

    PermissionResponse updatePermission(Long id, UpdatePermissionRequest request);

    void deletePermission(Long id);
}
