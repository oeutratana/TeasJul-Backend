package com.example.spring_boot_project_api.exception;

/**
 * Thrown when an OTP is invalid, expired, already consumed, or has exceeded
 * the maximum number of attempts. The message must never reveal whether an
 * account exists for enumeration-resilient flows.
 */
public class InvalidOtpException extends RuntimeException {

    public InvalidOtpException(String message) {
        super(message);
    }
}
