package com.example.spring_boot_project_api.service.impl;

import java.time.Duration;

import org.springframework.stereotype.Service;

import com.example.spring_boot_project_api.ratelimit.BucketStore;

/**
 * In-memory rate limiter used for per-account OTP / password-recovery /
 * per-IP throttling inside services (keys are typically "user:email:purpose").
 *
 * <p>Implementation delegates to the shared {@link BucketStore} so all limits
 * share one bounded, thread-safe engine. The public API is unchanged:
 * {@code true} means allowed and the request was recorded, {@code false} means
 * the limit in the window has been exceeded. This is a single-instance limiter;
 * for a multi-node deployment swap the {@link BucketStore} for a shared store
 * (e.g. Redis) without touching callers.
 */
@Service
public class RateLimiterService {

    private static final String LEGACY_KEY_PREFIX = "legacy:";
    private static final Duration DEFAULT_WINDOW = Duration.ofMinutes(15);

    private final BucketStore bucketStore;

    public RateLimiterService(BucketStore bucketStore) {
        this.bucketStore = bucketStore;
    }

    /**
     * Returns {@code true} if the key is allowed (i.e. request may proceed) and
     * records the request; {@code false} if the limit has been exceeded.
     */
    public boolean tryAcquire(String key, int maxRequests, Duration window) {
        return bucketStore.tryConsume(LEGACY_KEY_PREFIX + key, maxRequests, window).allowed();
    }

    public boolean tryAcquire(String key, int maxRequests) {
        return tryAcquire(key, maxRequests, DEFAULT_WINDOW);
    }
}