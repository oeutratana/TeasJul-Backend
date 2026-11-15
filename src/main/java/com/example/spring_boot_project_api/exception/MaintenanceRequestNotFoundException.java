package com.example.spring_boot_project_api.exception;

public class MaintenanceRequestNotFoundException extends RuntimeException {

    public MaintenanceRequestNotFoundException(String message) {
        super(message);
    }

    public static MaintenanceRequestNotFoundException forId(Long id) {
        return new MaintenanceRequestNotFoundException(
                "Maintenance request not found with id: " + id);
    }
}
