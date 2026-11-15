package com.example.spring_boot_project_api.service;

/**
 * Abstraction over the mail provider. Controllers/services never touch the
 * mail implementation directly.
 */
public interface EmailService {

    /**
     * Sends a recovery-email verification OTP to the given address.
     *
     * @param to      the recipient recovery email
     * @param otp     the six-digit code (delivered once, never persisted/logged)
     * @param expiresInMinutes how many minutes the code is valid for
     */
    void sendRecoveryEmailVerificationOtp(String to, String otp, int expiresInMinutes);

    /**
     * Sends a password-reset OTP to the given (verified) recovery email.
     */
    void sendPasswordResetOtp(String to, String otp, int expiresInMinutes);

    /**
     * Sends a 2FA login OTP to the given (verified) recovery email.
     */
    void sendTwoFactorOtp(String to, String otp, int expiresInMinutes);
}
