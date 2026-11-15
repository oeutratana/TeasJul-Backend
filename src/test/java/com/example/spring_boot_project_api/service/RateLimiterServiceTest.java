package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.example.spring_boot_project_api.ratelimit.CaffeineBucketStore;
import com.example.spring_boot_project_api.ratelimit.RateLimitProperties;
import com.example.spring_boot_project_api.service.impl.RateLimiterService;

/**
 * Guards the legacy {@link RateLimiterService} contract (used by OTP / account
 * security flows) after it was reimplemented on top of {@link CaffeineBucketStore}.
 */
class RateLimiterServiceTest {

    private final RateLimiterService rateLimiter =
            new RateLimiterService(new CaffeineBucketStore(new RateLimitProperties()));

    @Test
    void allowsUpToWindowCapacityThenDenies() {
        assertThat(rateLimiter.tryAcquire("user:1:forgot", 2, Duration.ofMinutes(5))).isTrue();
        assertThat(rateLimiter.tryAcquire("user:1:forgot", 2, Duration.ofMinutes(5))).isTrue();
        assertThat(rateLimiter.tryAcquire("user:1:forgot", 2, Duration.ofMinutes(5))).isFalse();
    }

    @Test
    void defaultOverloadUsesFifteenMinuteWindow() {
        assertThat(rateLimiter.tryAcquire("user:2:2fa", 1)).isTrue();
        assertThat(rateLimiter.tryAcquire("user:2:2fa", 1)).isFalse();
    }

    @Test
    void keysAreIndependent() {
        assertThat(rateLimiter.tryAcquire("a", 1)).isTrue();
        assertThat(rateLimiter.tryAcquire("a", 1)).isFalse();
        assertThat(rateLimiter.tryAcquire("b", 1)).isTrue();
    }

    @Test
    void releasesTokensAfterWindowElapses() throws Exception {
        assertThat(rateLimiter.tryAcquire("user:3:forgot", 1, Duration.ofMillis(300))).isTrue();
        assertThat(rateLimiter.tryAcquire("user:3:forgot", 1, Duration.ofMillis(300))).isFalse();
        Thread.sleep(400);
        assertThat(rateLimiter.tryAcquire("user:3:forgot", 1, Duration.ofMillis(300))).isTrue();
    }
}