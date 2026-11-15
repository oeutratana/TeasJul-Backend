package com.example.spring_boot_project_api.ratelimit;

import java.time.Duration;

/**
 * Storage abstraction for rate-limit buckets.
 *
 * <p>Separating the interface from the concrete store lets the in-memory
 * Caffeine-backed implementation be swapped for a shared store (e.g. Redis via
 * bucket4j's {@code Bucket4j} adapters) for multi-instance deployments with no
 * change to callers.
 */
public interface BucketStore {

    /**
     * Consumes one token from the bucket identified by {@code key} according to
     * the given capacity and refill window. Never throws on ordinary limiting
     * conditions.
     *
     * @param key          bucket key (already namespaced by the caller)
     * @param capacity     maximum tokens before refusal
     * @param refillPeriod window in which capacity tokens refill
     * @return whether the request is allowed plus retry hint when denied
     */
    BucketResult tryConsume(String key, long capacity, Duration refillPeriod);
}