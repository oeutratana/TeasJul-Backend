package com.example.spring_boot_project_api.exception;

public class UtilityTypeNotFoundException extends RuntimeException {

    public UtilityTypeNotFoundException(String message) {
        super(message);
    }

    public static UtilityTypeNotFoundException forId(Long id) {
        return new UtilityTypeNotFoundException("Utility type not found with id: " + id);
    }
}
