package com.example.spring_boot_project_api.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class RateLimitPolicyTest {

    @Test
    void allPoliciesHavePositiveCapacityAndRefillPeriod() {
        for (RateLimitPolicy policy : RateLimitPolicy.values()) {
            assertThat(policy.getCapacity())
                    .as("capacity of %s", policy.name())
                    .isPositive();
            assertThat(policy.getRefillPeriod())
                    .as("refill period of %s", policy.name())
                    .isNotNull()
                    .isNotEqualTo(Duration.ZERO);
        }
    }

    @Test
    void authPoliciesFailClosedForBruteForceProtection() {
        assertThat(RateLimitPolicy.AUTH_LOGIN_IP.isFailClosed()).isTrue();
        assertThat(RateLimitPolicy.AUTH_LOGIN_ACCOUNT.isFailClosed()).isTrue();
        assertThat(RateLimitPolicy.AUTH_REGISTER_IP.isFailClosed()).isTrue();
        assertThat(RateLimitPolicy.AUTH_OTP_IP.isFailClosed()).isTrue();
        assertThat(RateLimitPolicy.AUTH_OTP_ACCOUNT.isFailClosed()).isTrue();
        assertThat(RateLimitPolicy.AUTH_PASSWORD_RESET_IP.isFailClosed()).isTrue();
        assertThat(RateLimitPolicy.AUTH_PASSWORD_RESET_ACCOUNT.isFailClosed()).isTrue();
        assertThat(RateLimitPolicy.AUTH_2FA_IP.isFailClosed()).isTrue();
        assertThat(RateLimitPolicy.AUTH_2FA_ACCOUNT.isFailClosed()).isTrue();
    }

    @Test
    void businessPoliciesFailOpenSoTheProductStaysUp() {
        assertThat(RateLimitPolicy.PUBLIC_READ.isFailClosed()).isFalse();
        assertThat(RateLimitPolicy.AUTHENTICATED_API_IP.isFailClosed()).isFalse();
        assertThat(RateLimitPolicy.SENSITIVE_WRITE_USER.isFailClosed()).isFalse();
        assertThat(RateLimitPolicy.PAYMENT_CALLBACK.isFailClosed()).isFalse();
    }

    @Test
    void accountLimitsAreStricterThanIpLimits() {
        assertThat(RateLimitPolicy.AUTH_LOGIN_ACCOUNT.getCapacity())
                .isLessThan(RateLimitPolicy.AUTH_LOGIN_IP.getCapacity());
        assertThat(RateLimitPolicy.AUTH_OTP_ACCOUNT.getCapacity())
                .isLessThan(RateLimitPolicy.AUTH_OTP_IP.getCapacity());
        assertThat(RateLimitPolicy.AUTH_PASSWORD_RESET_ACCOUNT.getCapacity())
                .isLessThan(RateLimitPolicy.AUTH_PASSWORD_RESET_IP.getCapacity());
    }
}