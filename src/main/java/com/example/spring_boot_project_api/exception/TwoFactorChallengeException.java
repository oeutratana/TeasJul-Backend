package com.example.spring_boot_project_api.exception;

/**
 * Thrown when a 2FA challenge token is invalid, expired, or already consumed,
 * or when 2FA is required but a token is missing/mismatched.
 */
public class TwoFactorChallengeException extends RuntimeException {

    public TwoFactorChallengeException(String message) {
        super(message);
    }
}
