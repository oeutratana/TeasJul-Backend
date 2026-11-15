package com.example.spring_boot_project_api.exception;

/**
 * Thrown when a request is throttled because too many OTP/verify attempts were
 * made within a time window.
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}
