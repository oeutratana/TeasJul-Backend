package com.example.spring_boot_project_api.exception;

public class HouseNotFoundException extends RuntimeException {

    public HouseNotFoundException(String message) {
        super(message);
    }

    public static HouseNotFoundException forId(Long id) {
        return new HouseNotFoundException("House not found with id: " + id);
    }
}
