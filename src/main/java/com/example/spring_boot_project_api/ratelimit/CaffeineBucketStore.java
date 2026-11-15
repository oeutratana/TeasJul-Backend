package com.example.spring_boot_project_api.ratelimit;

import java.time.Duration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;

/**
 * Caffeine-backed in-memory {@link BucketStore}. Buckets are token buckets
 * from bucket4j (thread-safe, greedy refill) held in a bounded cache so the
 * total number of tracked buckets can never grow without limit.
 */
public class CaffeineBucketStore implements BucketStore {

    private final Cache<String, Bucket> buckets;

    public CaffeineBucketStore(RateLimitProperties properties) {
        RateLimitProperties.Store store = properties.getStore();
        this.buckets = Caffeine.newBuilder()
                .maximumSize(store.getMaxBuckets())
                .expireAfterAccess(store.getBucketIdle())
                .expireAfterWrite(store.getBucketMaxLife())
                .build();
    }

    @Override
    public BucketResult tryConsume(String key, long capacity, Duration refillPeriod) {
        return consume(key, capacity, refillPeriod);
    }

    private BucketResult consume(String key, long capacity, Duration refillPeriod) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Rate-limit capacity must be positive");
        }
        if (refillPeriod == null || refillPeriod.isNegative() || refillPeriod.isZero()) {
            throw new IllegalArgumentException("Rate-limit refill period must be a positive duration");
        }
        Bucket bucket = buckets.get(key, k -> Bucket.builder()
                .addLimit(Bandwidth.classic(capacity, Refill.greedy(capacity, refillPeriod)))
                .build());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return BucketResult.allow();
        }
        return BucketResult.deny(Duration.ofNanos(probe.getNanosToWaitForRefill()));
    }
}