package com.example.spring_boot_project_api.ratelimit;

import com.example.spring_boot_project_api.service.LoginRateLimitSettings;

/**
 * Supplies the SUPER_ADMIN-configurable login rate-limiting settings to the
 * existing limiter.
 *
 * <p>This is configuration plumbing only — it decides neither which requests
 * are rejected nor how buckets are counted. Enforcement stays in
 * {@link RateLimitingFilter} / {@link EffectivePolicyResolver}, so the feature
 * adds no second rate-limiting mechanism.
 *
 * <p>{@link RateLimitingFilter} reads the settings once per request and hands the
 * same snapshot to {@link EffectivePolicyResolver}, so serving a request costs
 * a single settings lookup — cheap on the login path, which is by definition
 * rare, and it buys immediate effect after a settings change plus survival
 * across restarts without a second cache to invalidate.
 */
public interface LoginRateLimitSettingsSource {

    /**
     * @return the settings currently in force; never {@code null}
     */
    LoginRateLimitSettings current();

    /**
     * @return whether {@code policy} is one of the login policies the
     *         SUPER_ADMIN screen governs
     */
    static boolean isLoginPolicy(RateLimitPolicy policy) {
        return policy == RateLimitPolicy.AUTH_LOGIN_IP
                || policy == RateLimitPolicy.AUTH_LOGIN_ACCOUNT;
    }
}
