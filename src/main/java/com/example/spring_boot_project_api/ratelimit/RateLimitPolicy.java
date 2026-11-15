package com.example.spring_boot_project_api.ratelimit;

import java.time.Duration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Named rate-limit policies applied by {@link RateLimitingFilter}.
 *
 * <p>Each policy declares a sensible default capacity and refill period plus a
 * fail-safe mode:
 * <ul>
 *   <li>{@code failClosed} — if the underlying store fails, the request is
 *       denied (503). Used for authentication flows where brutal brute-force
 *       protection matters more than short availability glitches.</li>
 *   <li>{@code failOpen} — if the store fails, the request is allowed through
 *       and the failure logged. Used for ordinary business/public APIs so a
 *       cache hiccup can never take the whole product down.</li>
 * </ul>
 *
 * <p>All defaults (capacity / period / failClosed) can be overridden per policy
 * via {@code app.ratelimit.policies.<NAME>.*} in {@code application.properties}.
 */
@Getter
@RequiredArgsConstructor
public enum RateLimitPolicy {

    AUTH_LOGIN_IP(20, Duration.ofMinutes(15), true),
    AUTH_LOGIN_ACCOUNT(5, Duration.ofMinutes(15), true),
    AUTH_REGISTER_IP(10, Duration.ofHours(1), true),

    AUTH_OTP_IP(30, Duration.ofMinutes(15), true),
    AUTH_OTP_ACCOUNT(8, Duration.ofMinutes(15), true),

    AUTH_PASSWORD_RESET_IP(10, Duration.ofMinutes(15), true),
    AUTH_PASSWORD_RESET_ACCOUNT(5, Duration.ofMinutes(15), true),

    AUTH_2FA_IP(20, Duration.ofMinutes(15), true),
    AUTH_2FA_ACCOUNT(8, Duration.ofMinutes(15), true),

    PUBLIC_READ(120, Duration.ofMinutes(1), false),
    PUBLIC_DETAIL(60, Duration.ofMinutes(1), false),
    PUBLIC_CONTACT(10, Duration.ofHours(1), false),

    AUTHENTICATED_API_IP(1200, Duration.ofMinutes(1), false),
    AUTHENTICATED_API_USER(600, Duration.ofMinutes(1), false),

    SENSITIVE_WRITE_USER(30, Duration.ofMinutes(1), false),

    /**
     * PayWay points its HMAC-signed callback here. The signature is the real
     * authorization; the limit is generous and IP-anchored so legitimate
     * callbacks are never dropped.
     */
    PAYMENT_CALLBACK(120, Duration.ofMinutes(1), false);

    private final long capacity;
    private final Duration refillPeriod;
    private final boolean failClosed;
}