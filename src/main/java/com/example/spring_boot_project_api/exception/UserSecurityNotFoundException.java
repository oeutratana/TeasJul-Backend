package com.example.spring_boot_project_api.exception;

/**
 * Thrown when a user-security record is required but has not been initialized
 * for a user (e.g. enabling 2FA before any security setup exists).
 */
public class UserSecurityNotFoundException extends RuntimeException {

    public UserSecurityNotFoundException(String message) {
        super(message);
    }
}
