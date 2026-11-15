package com.example.spring_boot_project_api.exception;

public class MaintenanceAssignmentNotFoundException extends RuntimeException {

    public MaintenanceAssignmentNotFoundException(String message) {
        super(message);
    }

    public static MaintenanceAssignmentNotFoundException forId(Long id) {
        return new MaintenanceAssignmentNotFoundException(
                "Maintenance assignment not found with id: " + id);
    }
}
