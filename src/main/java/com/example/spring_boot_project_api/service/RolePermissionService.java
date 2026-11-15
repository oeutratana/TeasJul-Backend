package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.ReplaceRolePermissionsRequest;
import com.example.spring_boot_project_api.dto.response.PermissionResponse;

public interface RolePermissionService {

    List<PermissionResponse> getRolePermissions(Long roleId);

    PermissionResponse assignPermissionToRole(Long roleId, Long permissionId);

    void removePermissionFromRole(Long roleId, Long permissionId);

    List<PermissionResponse> replaceRolePermissions(Long roleId, ReplaceRolePermissionsRequest request);
}
