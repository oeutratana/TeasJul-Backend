package com.example.spring_boot_project_api.exception;

public class RolePermissionNotFoundException extends RuntimeException {

    public RolePermissionNotFoundException(String message) {
        super(message);
    }

    public static RolePermissionNotFoundException forIds(Long roleId, Long permissionId) {
        return new RolePermissionNotFoundException(
                "Permission " + permissionId + " is not assigned to role " + roleId);
    }
}
