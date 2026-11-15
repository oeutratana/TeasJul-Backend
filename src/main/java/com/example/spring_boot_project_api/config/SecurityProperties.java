package com.example.spring_boot_project_api.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurable security-related settings (OTP, rate limiting, 2FA policy).
 * All values are read from environment/configuration, never hardcoded.
 */
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    /** OTP lifetime in seconds. */
    private Duration otpTtl = Duration.ofMinutes(5);

    /** Maximum failed verification attempts before an OTP is invalidated. */
    private int otpMaxAttempts = 5;

    /** Cooldown before a new OTP can be requested for the same purpose/account. */
    private Duration otpResendCooldown = Duration.ofSeconds(60);

    /** Maximum OTPs that can be sent per account/purpose within the window. */
    private int otpMaxSendsPerWindow = 5;

    /** Window over which the send limit applies. */
    private Duration otpSendWindow = Duration.ofMinutes(15);

    /** Maximum requests per IP within the rate-limit window. */
    private int maxRequestsPerIp = 10;

    /** Rate-limit window for per-IP limits. */
    private Duration rateLimitWindow = Duration.ofMinutes(15);

    /** Roles that always require 2FA (role codes). Applied on the backend. */
    private List<String> twoFactorRequiredRoles = List.of();

    /** Lifetime of a password-reset token (after OTP verification). */
    private Duration resetTokenTtl = Duration.ofMinutes(5);

    /** Lifetime of the 2FA challenge token (after valid credentials). */
    private Duration challengeTokenTtl = Duration.ofMinutes(5);

    public Duration getOtpTtl() {
        return otpTtl;
    }

    public void setOtpTtl(Duration otpTtl) {
        this.otpTtl = otpTtl;
    }

    public int getOtpMaxAttempts() {
        return otpMaxAttempts;
    }

    public void setOtpMaxAttempts(int otpMaxAttempts) {
        this.otpMaxAttempts = otpMaxAttempts;
    }

    public Duration getOtpResendCooldown() {
        return otpResendCooldown;
    }

    public void setOtpResendCooldown(Duration otpResendCooldown) {
        this.otpResendCooldown = otpResendCooldown;
    }

    public int getOtpMaxSendsPerWindow() {
        return otpMaxSendsPerWindow;
    }

    public void setOtpMaxSendsPerWindow(int otpMaxSendsPerWindow) {
        this.otpMaxSendsPerWindow = otpMaxSendsPerWindow;
    }

    public Duration getOtpSendWindow() {
        return otpSendWindow;
    }

    public void setOtpSendWindow(Duration otpSendWindow) {
        this.otpSendWindow = otpSendWindow;
    }

    public int getMaxRequestsPerIp() {
        return maxRequestsPerIp;
    }

    public void setMaxRequestsPerIp(int maxRequestsPerIp) {
        this.maxRequestsPerIp = maxRequestsPerIp;
    }

    public Duration getRateLimitWindow() {
        return rateLimitWindow;
    }

    public void setRateLimitWindow(Duration rateLimitWindow) {
        this.rateLimitWindow = rateLimitWindow;
    }

    public List<String> getTwoFactorRequiredRoles() {
        return twoFactorRequiredRoles;
    }

    public void setTwoFactorRequiredRoles(List<String> twoFactorRequiredRoles) {
        this.twoFactorRequiredRoles = twoFactorRequiredRoles;
    }

    public Duration getResetTokenTtl() {
        return resetTokenTtl;
    }

    public void setResetTokenTtl(Duration resetTokenTtl) {
        this.resetTokenTtl = resetTokenTtl;
    }

    public Duration getChallengeTokenTtl() {
        return challengeTokenTtl;
    }

    public void setChallengeTokenTtl(Duration challengeTokenTtl) {
        this.challengeTokenTtl = challengeTokenTtl;
    }
}
