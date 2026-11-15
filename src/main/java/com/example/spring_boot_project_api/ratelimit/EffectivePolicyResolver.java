package com.example.spring_boot_project_api.ratelimit;

import java.time.Duration;
import java.util.Objects;

import com.example.spring_boot_project_api.service.LoginRateLimitSettings;

/**
 * Resolves the effective enforcement parameters for a {@link RateLimitPolicy},
 * applying optional operator overrides from
 * {@code app.ratelimit.policies.<NAME>.{capacity,refill-period,fail-closed}}.
 * Unset override fields fall back to the enum defaults.
 *
 * <p>For the login policies the SUPER_ADMIN-managed settings win over
 * {@code application.properties}, because they are the runtime source of truth
 * exposed on the settings screen:
 * <ul>
 *   <li>{@code AUTH_LOGIN_ACCOUNT} — capacity is {@code maxFailedAttempts} and
 *       the refill period is {@code lockoutDurationMinutes}, i.e. "this account
 *       may try N times, then is locked out for M minutes";</li>
 *   <li>{@code AUTH_LOGIN_IP} — keeps its configured capacity: it is a
 *       per-IP scraping guard, not the per-account lockout the screen edits.</li>
 * </ul>
 *
 * <p>When login rate limiting is switched off, the login policies are left
 * untouched here and {@link RateLimitingFilter} skips them entirely, so no
 * partial enforcement is possible.
 */
public class EffectivePolicyResolver {

    private final RateLimitProperties properties;
    private final LoginRateLimitSettingsSource loginSettingsSource;

    public EffectivePolicyResolver(RateLimitProperties properties,
            LoginRateLimitSettingsSource loginSettingsSource) {
        this.properties = properties;
        this.loginSettingsSource = Objects.requireNonNull(loginSettingsSource,
                "loginSettingsSource is required");
    }

    public EffectiveRateLimitPolicy forPolicy(RateLimitPolicy policy) {
        return forPolicy(policy, loginSettingsSource.current());
    }

    /**
     * @param loginSettings the already-read settings, so a request served with
     *                      several checks performs a single settings lookup
     */
    public EffectiveRateLimitPolicy forPolicy(RateLimitPolicy policy, LoginRateLimitSettings loginSettings) {
        EffectiveRateLimitPolicy resolved = fromProperties(policy);
        if (!LoginRateLimitSettingsSource.isLoginPolicy(policy)
                || loginSettings == null
                || !loginSettings.enabled()) {
            return resolved;
        }
        Duration lockout = Duration.ofMinutes(loginSettings.lockoutDurationMinutes());
        return switch (policy) {
            case AUTH_LOGIN_ACCOUNT -> new EffectiveRateLimitPolicy(
                    policy, loginSettings.maxFailedAttempts(), lockout, resolved.failClosed());
            default -> resolved;
        };
    }

    private EffectiveRateLimitPolicy fromProperties(RateLimitPolicy policy) {
        RateLimitProperties.Policy override = properties.getPolicies().get(policy.name());
        if (override == null) {
            return EffectiveRateLimitPolicy.defaults(policy);
        }
        long capacity = override.getCapacity() != null ? override.getCapacity() : policy.getCapacity();
        Duration refillPeriod = override.getRefillPeriod() != null
                ? override.getRefillPeriod() : policy.getRefillPeriod();
        boolean failClosed = override.getFailClosed() != null
                ? override.getFailClosed() : policy.isFailClosed();
        return new EffectiveRateLimitPolicy(policy, capacity, refillPeriod, failClosed);
    }
}
