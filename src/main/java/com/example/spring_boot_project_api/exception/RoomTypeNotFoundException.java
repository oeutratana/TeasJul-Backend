package com.example.spring_boot_project_api.exception;

public class RoomTypeNotFoundException extends RuntimeException {

    public RoomTypeNotFoundException(String message) {
        super(message);
    }

    public static RoomTypeNotFoundException forId(Long id) {
        return new RoomTypeNotFoundException("Room type not found with id: " + id);
    }
}
