package com.example.spring_boot_project_api.exception;

public class UtilityMeterNotFoundException extends RuntimeException {

    public UtilityMeterNotFoundException(String message) {
        super(message);
    }

    public static UtilityMeterNotFoundException forId(Long id) {
        return new UtilityMeterNotFoundException("Utility meter not found with id: " + id);
    }
}
