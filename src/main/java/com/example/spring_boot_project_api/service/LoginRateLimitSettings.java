package com.example.spring_boot_project_api.service;

/**
 * Immutable snapshot of the SUPER_ADMIN-configurable login rate-limiting
 * settings, as persisted in the existing {@code system_settings} table under
 * the keys {@code LOGIN_RATE_LIMIT_ENABLED}, {@code LOGIN_MAX_FAILED_ATTEMPTS}
 * and {@code LOGIN_LOCKOUT_DURATION_MINUTES}.
 *
 * <p>This is the single source of truth consumed by the existing login rate
 * limiter ({@code AUTH_LOGIN_IP} / {@code AUTH_LOGIN_ACCOUNT} policies). It is a
 * plain value carrier on purpose: no persistence, no caching and no rate
 * limiting logic of its own, so the limiter keeps exactly one enforcement
 * mechanism.
 *
 * @param enabled               whether login rate limiting is enforced
 * @param maxFailedAttempts     per-account attempts allowed inside the lockout window
 * @param lockoutDurationMinutes length of the lockout/refill window in minutes
 */
public record LoginRateLimitSettings(
        boolean enabled,
        int maxFailedAttempts,
        int lockoutDurationMinutes
) {
}
