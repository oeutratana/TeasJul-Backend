package com.example.spring_boot_project_api.exception;

public class UtilityRateNotFoundException extends RuntimeException {

    public UtilityRateNotFoundException(String message) {
        super(message);
    }

    public static UtilityRateNotFoundException forId(Long id) {
        return new UtilityRateNotFoundException("Utility rate not found with id: " + id);
    }
}
