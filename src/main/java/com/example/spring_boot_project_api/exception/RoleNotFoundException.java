package com.example.spring_boot_project_api.exception;

public class RoleNotFoundException extends RuntimeException {

    public RoleNotFoundException(String message) {
        super(message);
    }

    public static RoleNotFoundException forId(Long id) {
        return new RoleNotFoundException("Role not found with id: " + id);
    }
}
