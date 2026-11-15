package com.example.spring_boot_project_api.exception;

public class SystemDefaultNotFoundException extends RuntimeException {

    public SystemDefaultNotFoundException(String message) {
        super(message);
    }

    public static SystemDefaultNotFoundException forId(Long id) {
        return new SystemDefaultNotFoundException("System default not found with id: " + id);
    }
}