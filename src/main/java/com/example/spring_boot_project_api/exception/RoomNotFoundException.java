package com.example.spring_boot_project_api.exception;

public class RoomNotFoundException extends RuntimeException {

    public RoomNotFoundException(String message) {
        super(message);
    }

    public static RoomNotFoundException forId(Long id) {
        return new RoomNotFoundException("Room not found with id: " + id);
    }
}
