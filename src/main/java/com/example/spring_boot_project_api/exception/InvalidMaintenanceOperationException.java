package com.example.spring_boot_project_api.exception;

public class InvalidMaintenanceOperationException extends RuntimeException {

    public InvalidMaintenanceOperationException(String message) {
        super(message);
    }
}
