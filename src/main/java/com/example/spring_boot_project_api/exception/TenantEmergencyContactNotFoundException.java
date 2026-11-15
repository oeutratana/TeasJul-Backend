package com.example.spring_boot_project_api.exception;

public class TenantEmergencyContactNotFoundException extends RuntimeException {

    public TenantEmergencyContactNotFoundException(String message) {
        super(message);
    }

    public static TenantEmergencyContactNotFoundException forId(Long id) {
        return new TenantEmergencyContactNotFoundException(
                "Tenant emergency contact not found with id: " + id);
    }
}
