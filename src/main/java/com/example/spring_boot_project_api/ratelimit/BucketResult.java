package com.example.spring_boot_project_api.ratelimit;

import java.time.Duration;

/**
 * Result of a rate-limit probe.
 *
 * @param allowed    whether the request may proceed
 * @param retryAfter suggested wait before retrying, non-zero when denied
 */
public record BucketResult(boolean allowed, Duration retryAfter) {

    public static BucketResult allow() {
        return new BucketResult(true, Duration.ZERO);
    }

    public static BucketResult deny(Duration retryAfter) {
        return new BucketResult(false, retryAfter);
    }
}