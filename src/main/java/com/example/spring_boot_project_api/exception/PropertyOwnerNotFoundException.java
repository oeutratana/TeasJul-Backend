package com.example.spring_boot_project_api.exception;

public class PropertyOwnerNotFoundException extends RuntimeException {

    public PropertyOwnerNotFoundException(String message) {
        super(message);
    }

    public static PropertyOwnerNotFoundException forId(Long id) {
        return new PropertyOwnerNotFoundException("Property owner not found with id: " + id);
    }
}