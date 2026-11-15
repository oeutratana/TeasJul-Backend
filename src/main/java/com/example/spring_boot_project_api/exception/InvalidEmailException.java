package com.example.spring_boot_project_api.exception;

/**
 * Thrown when an email fails account-type-specific validation, e.g. an internal
 * (non-TENANT) user is given a login email that is not on the corporate domain.
 */
public class InvalidEmailException extends RuntimeException {
    public InvalidEmailException(String message) {
        super(message);
    }
}
