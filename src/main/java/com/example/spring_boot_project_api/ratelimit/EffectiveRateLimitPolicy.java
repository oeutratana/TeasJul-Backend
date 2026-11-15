package com.example.spring_boot_project_api.ratelimit;

import java.time.Duration;

/**
 * A {@link RateLimitPolicy} with any {@code app.ratelimit.policies.*}
 * overrides applied. This is what the limiter actually enforces.
 *
 * @param policy       the base policy (name is used for bucket key namespaces)
 * @param capacity     effective bucket capacity
 * @param refillPeriod effective refill window
 * @param failClosed   effective fail-safe mode
 */
public record EffectiveRateLimitPolicy(
        RateLimitPolicy policy,
        long capacity,
        Duration refillPeriod,
        boolean failClosed) {

    public static EffectiveRateLimitPolicy defaults(RateLimitPolicy policy) {
        return new EffectiveRateLimitPolicy(
                policy, policy.getCapacity(), policy.getRefillPeriod(), policy.isFailClosed());
    }
}