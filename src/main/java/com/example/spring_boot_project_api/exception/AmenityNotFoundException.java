package com.example.spring_boot_project_api.exception;

public class AmenityNotFoundException extends RuntimeException {

    public AmenityNotFoundException(String message) {
        super(message);
    }

    public static AmenityNotFoundException forId(Long id) {
        return new AmenityNotFoundException("Amenity not found with id: " + id);
    }
}
