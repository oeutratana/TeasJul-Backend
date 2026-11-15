package com.example.spring_boot_project_api.exception;

public class InvalidRoomBookingOperationException extends RuntimeException {
    public InvalidRoomBookingOperationException(String message) {
        super(message);
    }
}
