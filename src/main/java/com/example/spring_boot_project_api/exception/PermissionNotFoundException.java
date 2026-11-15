package com.example.spring_boot_project_api.exception;

public class PermissionNotFoundException extends RuntimeException {

    public PermissionNotFoundException(String message) {
        super(message);
    }

    public static PermissionNotFoundException forId(Long id) {
        return new PermissionNotFoundException("Permission not found with id: " + id);
    }
}
