package com.example.spring_boot_project_api.exception;

public class UtilityReadingNotFoundException extends RuntimeException {

    public UtilityReadingNotFoundException(String message) {
        super(message);
    }

    public static UtilityReadingNotFoundException forId(Long id) {
        return new UtilityReadingNotFoundException("Utility reading not found with id: " + id);
    }
}
