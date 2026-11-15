package com.example.spring_boot_project_api.exception;

public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException(String message) {
        super(message);
    }

    public static NotificationNotFoundException forId(Long id) {
        return new NotificationNotFoundException("Notification not found with id: " + id);
    }
}