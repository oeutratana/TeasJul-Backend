package com.example.spring_boot_project_api.exception;

public class MaintenanceCostNotFoundException extends RuntimeException {

    public MaintenanceCostNotFoundException(String message) {
        super(message);
    }

    public static MaintenanceCostNotFoundException forId(Long id) {
        return new MaintenanceCostNotFoundException(
                "Maintenance cost not found with id: " + id);
    }
}
