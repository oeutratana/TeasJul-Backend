package com.example.spring_boot_project_api.exception;

public class TenantNotFoundException extends RuntimeException {

    public TenantNotFoundException(String message) {
        super(message);
    }

    public static TenantNotFoundException forId(Long id) {
        return new TenantNotFoundException("Tenant not found with id: " + id);
    }
}
