package com.example.spring_boot_project_api.exception;

/**
 * Thrown when a new password violates the password policy (e.g. equals the
 * system email, too weak, or confirmation mismatch).
 */
public class InvalidPasswordException extends RuntimeException {

    public InvalidPasswordException(String message) {
        super(message);
    }
}
