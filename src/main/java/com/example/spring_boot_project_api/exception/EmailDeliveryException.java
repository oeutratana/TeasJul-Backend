package com.example.spring_boot_project_api.exception;

/**
 * Thrown when an outbound email (OTP recovery, password reset, 2FA) could not be
 * delivered due to a mail-provider failure. Message is intentionally generic and
 * contains no sensitive material.
 */
public class EmailDeliveryException extends RuntimeException {
    public EmailDeliveryException(String message) {
        super(message);
    }
}
