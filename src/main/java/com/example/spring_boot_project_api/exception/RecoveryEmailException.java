package com.example.spring_boot_project_api.exception;

/**
 * Thrown when the recovery email cannot be used (not configured, not verified,
 * or the operation requires a currently authenticated and verified setup).
 */
public class RecoveryEmailException extends RuntimeException {

    public RecoveryEmailException(String message) {
        super(message);
    }
}
