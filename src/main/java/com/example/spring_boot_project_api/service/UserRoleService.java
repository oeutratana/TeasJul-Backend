package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.response.RoleResponse;

public interface UserRoleService {

    List<RoleResponse> getUserRoles(Long userId);

    List<RoleResponse> getCurrentUserRoles();

    RoleResponse assignRoleToUser(Long userId, Long roleId);

    void removeRoleFromUser(Long userId, Long roleId);
}
