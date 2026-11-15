package com.example.spring_boot_project_api.exception;

public class RoomBookingNotFoundException extends RuntimeException {
    public RoomBookingNotFoundException(String message) {
        super(message);
    }

    public static RoomBookingNotFoundException forId(Long id) {
        return new RoomBookingNotFoundException("Room booking not found with id: " + id);
    }
}
