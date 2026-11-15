package com.example.spring_boot_project_api.exception;

public class MaintenanceCategoryNotFoundException extends RuntimeException {

    public MaintenanceCategoryNotFoundException(String message) {
        super(message);
    }

    public static MaintenanceCategoryNotFoundException forId(Long id) {
        return new MaintenanceCategoryNotFoundException(
                "Maintenance category not found with id: " + id);
    }
}
