package com.example.spring_boot_project_api.exception;

/**
 * Thrown when two-factor authentication cannot be enabled/disabled for the
 * current user or role configuration.
 */
public class TwoFactorException extends RuntimeException {

    public TwoFactorException(String message) {
        super(message);
    }
}
