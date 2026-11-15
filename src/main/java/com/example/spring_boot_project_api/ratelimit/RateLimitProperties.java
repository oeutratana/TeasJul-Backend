package com.example.spring_boot_project_api.ratelimit;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the API rate limiter.
 *
 * <p>Prefix {@code app.ratelimit}. All values may be set through environment
 * variables (via {@code application.properties}), so deployment environments
 * can tune limits without code changes.
 *
 * <p>{@code policies} is an optional override map keyed by
 * {@link RateLimitPolicy} enum name; each entry overrides only the fields the
 * operator wants to change.
 */
@ConfigurationProperties(prefix = "app.ratelimit")
public class RateLimitProperties {

    /** Master switch; disables all filter-level limiting when false. */
    private boolean enabled = true;

    private Store store = new Store();

    private Proxy proxy = new Proxy();

    /** Per-policy overrides, keyed by {@link RateLimitPolicy} enum name. */
    private java.util.Map<String, Policy> policies = new java.util.HashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
    }

    public Proxy getProxy() {
        return proxy;
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    public java.util.Map<String, Policy> getPolicies() {
        return policies;
    }

    public void setPolicies(java.util.Map<String, Policy> policies) {
        this.policies = policies;
    }

    /** Bounds for the in-memory bucket cache so it can never grow unbounded. */
    public static class Store {

        /** Hard cap on the number of concurrently tracked buckets. */
        private long maxBuckets = 100_000;

        /** Buckets are evicted once idle for this long. */
        private Duration bucketIdle = Duration.ofMinutes(30);

        /** Hard life cap per bucket regardless of activity. */
        private Duration bucketMaxLife = Duration.ofHours(1);

        public long getMaxBuckets() {
            return maxBuckets;
        }

        public void setMaxBuckets(long maxBuckets) {
            this.maxBuckets = maxBuckets;
        }

        public Duration getBucketIdle() {
            return bucketIdle;
        }

        public void setBucketIdle(Duration bucketIdle) {
            this.bucketIdle = bucketIdle;
        }

        public Duration getBucketMaxLife() {
            return bucketMaxLife;
        }

        public void setBucketMaxLife(Duration bucketMaxLife) {
            this.bucketMaxLife = bucketMaxLife;
        }
    }

    /**
     * Reverse-proxy handling. Forwarded headers are only honored when the
     * operator explicitly opts in and keeps the trusted proxy configuration
     * in sync; otherwise {@code X-Forwarded-For} is never blindly trusted
     * because a client can spoof it.
     */
    public static class Proxy {

        private boolean trustForwardedHeaders = false;

        private String forwardedHeader = "X-Forwarded-For";

        public boolean isTrustForwardedHeaders() {
            return trustForwardedHeaders;
        }

        public void setTrustForwardedHeaders(boolean trustForwardedHeaders) {
            this.trustForwardedHeaders = trustForwardedHeaders;
        }

        public String getForwardedHeader() {
            return forwardedHeader;
        }

        public void setForwardedHeader(String forwardedHeader) {
            this.forwardedHeader = forwardedHeader;
        }
    }

    /** Overrides for a single {@link RateLimitPolicy}. */
    public static class Policy {

        private Long capacity;

        private Duration refillPeriod;

        private Boolean failClosed;

        public Long getCapacity() {
            return capacity;
        }

        public void setCapacity(Long capacity) {
            this.capacity = capacity;
        }

        public Duration getRefillPeriod() {
            return refillPeriod;
        }

        public void setRefillPeriod(Duration refillPeriod) {
            this.refillPeriod = refillPeriod;
        }

        public Boolean getFailClosed() {
            return failClosed;
        }

        public void setFailClosed(Boolean failClosed) {
            this.failClosed = failClosed;
        }
    }
}