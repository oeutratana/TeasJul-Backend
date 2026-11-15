package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.CreateRoleRequest;
import com.example.spring_boot_project_api.dto.request.UpdateRoleRequest;
import com.example.spring_boot_project_api.dto.response.RoleResponse;

public interface RoleService {

    RoleResponse createRole(CreateRoleRequest request);

    RoleResponse getRoleById(Long id);

    List<RoleResponse> getAllRoles();

    RoleResponse updateRole(Long id, UpdateRoleRequest request);

    void deleteRole(Long id);
}
