package com.example.spring_boot_project_api.exception;

/**
 * Thrown when a password-reset token (the short-lived authorization obtained
 * after OTP verification) is invalid, expired, already used, or mismatched.
 */
public class InvalidResetTokenException extends RuntimeException {

    public InvalidResetTokenException(String message) {
        super(message);
    }
}
