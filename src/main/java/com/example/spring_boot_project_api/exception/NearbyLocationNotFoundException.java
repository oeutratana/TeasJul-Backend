package com.example.spring_boot_project_api.exception;

public class NearbyLocationNotFoundException extends RuntimeException {

    public NearbyLocationNotFoundException(String message) {
        super(message);
    }

    public static NearbyLocationNotFoundException forId(Long id) {
        return new NearbyLocationNotFoundException("Nearby location not found with id: " + id);
    }
}