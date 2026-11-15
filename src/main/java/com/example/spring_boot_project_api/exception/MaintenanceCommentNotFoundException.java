package com.example.spring_boot_project_api.exception;

public class MaintenanceCommentNotFoundException extends RuntimeException {

    public MaintenanceCommentNotFoundException(String message) {
        super(message);
    }

    public static MaintenanceCommentNotFoundException forId(Long id) {
        return new MaintenanceCommentNotFoundException(
                "Maintenance comment not found with id: " + id);
    }
}
