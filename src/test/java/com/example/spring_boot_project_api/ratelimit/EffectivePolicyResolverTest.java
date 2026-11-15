package com.example.spring_boot_project_api.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.example.spring_boot_project_api.service.LoginRateLimitSettings;

class EffectivePolicyResolverTest {

    private final RateLimitProperties properties = new RateLimitProperties();
    private LoginRateLimitSettings loginSettings = new LoginRateLimitSettings(true, 5, 15);
    private final LoginRateLimitSettingsSource loginSettingsSource = () -> loginSettings;
    private final EffectivePolicyResolver resolver = new EffectivePolicyResolver(properties, loginSettingsSource);

    @Test
    void returnsEnumDefaultsWhenNoOverridesConfigured() {
        EffectiveRateLimitPolicy effective = resolver.forPolicy(RateLimitPolicy.AUTH_LOGIN_IP);
        assertThat(effective.capacity()).isEqualTo(RateLimitPolicy.AUTH_LOGIN_IP.getCapacity());
        assertThat(effective.refillPeriod()).isEqualTo(RateLimitPolicy.AUTH_LOGIN_IP.getRefillPeriod());
        assertThat(effective.failClosed()).isEqualTo(RateLimitPolicy.AUTH_LOGIN_IP.isFailClosed());
        assertThat(effective.policy()).isEqualTo(RateLimitPolicy.AUTH_LOGIN_IP);
    }

    @Test
    void appliesFullOverride() {
        RateLimitProperties.Policy override = new RateLimitProperties.Policy();
        override.setCapacity(3L);
        override.setRefillPeriod(Duration.ofMinutes(2));
        override.setFailClosed(false);
        properties.getPolicies().put("AUTH_LOGIN_IP", override);

        EffectiveRateLimitPolicy effective = resolver.forPolicy(RateLimitPolicy.AUTH_LOGIN_IP);
        assertThat(effective.capacity()).isEqualTo(3);
        assertThat(effective.refillPeriod()).isEqualTo(Duration.ofMinutes(2));
        assertThat(effective.failClosed()).isFalse();
    }

    @Test
    void appliesPartialOverrideAndKeepsOtherDefaults() {
        RateLimitProperties.Policy override = new RateLimitProperties.Policy();
        override.setCapacity(7L);
        properties.getPolicies().put("AUTH_OTP_ACCOUNT", override);

        EffectiveRateLimitPolicy effective = resolver.forPolicy(RateLimitPolicy.AUTH_OTP_ACCOUNT);
        assertThat(effective.capacity()).isEqualTo(7);
        assertThat(effective.refillPeriod()).isEqualTo(RateLimitPolicy.AUTH_OTP_ACCOUNT.getRefillPeriod());
        assertThat(effective.failClosed()).isEqualTo(RateLimitPolicy.AUTH_OTP_ACCOUNT.isFailClosed());
    }

    @Test
    void overridesAreCaseSensitiveByEnumNameAndOnlyForTarget() {
        RateLimitProperties.Policy override = new RateLimitProperties.Policy();
        override.setCapacity(9L);
        properties.getPolicies().put("PAYMENT_CALLBACK", override);

        assertThat(resolver.forPolicy(RateLimitPolicy.PAYMENT_CALLBACK).capacity()).isEqualTo(9);
        assertThat(resolver.forPolicy(RateLimitPolicy.AUTHENTICATED_API_IP).capacity())
                .isEqualTo(RateLimitPolicy.AUTHENTICATED_API_IP.getCapacity());
    }

    @Test
    void loginAccountPolicyUsesConfiguredAttemptsAndLockoutWindow() {
        loginSettings = new LoginRateLimitSettings(true, 3, 45);
        RateLimitProperties.Policy override = new RateLimitProperties.Policy();
        override.setCapacity(99L);
        override.setRefillPeriod(Duration.ofMinutes(1));
        properties.getPolicies().put("AUTH_LOGIN_ACCOUNT", override);

        EffectiveRateLimitPolicy effective = resolver.forPolicy(RateLimitPolicy.AUTH_LOGIN_ACCOUNT);

        assertThat(effective.capacity()).isEqualTo(3);
        assertThat(effective.refillPeriod()).isEqualTo(Duration.ofMinutes(45));
        assertThat(effective.failClosed()).isTrue();
    }

    @Test
    void loginIpPolicyKeepsItsConfiguredScrapingCapacity() {
        loginSettings = new LoginRateLimitSettings(true, 3, 45);
        RateLimitProperties.Policy override = new RateLimitProperties.Policy();
        override.setCapacity(11L);
        properties.getPolicies().put("AUTH_LOGIN_IP", override);

        EffectiveRateLimitPolicy effective = resolver.forPolicy(RateLimitPolicy.AUTH_LOGIN_IP);

        assertThat(effective.capacity()).isEqualTo(11);
        assertThat(effective.refillPeriod()).isEqualTo(RateLimitPolicy.AUTH_LOGIN_IP.getRefillPeriod());
    }

    @Test
    void nonLoginAuthPoliciesIgnoreLoginSettings() {
        loginSettings = new LoginRateLimitSettings(true, 3, 45);

        EffectiveRateLimitPolicy effective = resolver.forPolicy(RateLimitPolicy.AUTH_OTP_ACCOUNT);

        assertThat(effective.capacity()).isEqualTo(RateLimitPolicy.AUTH_OTP_ACCOUNT.getCapacity());
        assertThat(effective.refillPeriod()).isEqualTo(RateLimitPolicy.AUTH_OTP_ACCOUNT.getRefillPeriod());
    }

    @Test
    void loginAccountPolicyFallsBackToPropertiesWhenLoginLimitingDisabled() {
        loginSettings = new LoginRateLimitSettings(false, 3, 45);
        RateLimitProperties.Policy override = new RateLimitProperties.Policy();
        override.setCapacity(8L);
        properties.getPolicies().put("AUTH_LOGIN_ACCOUNT", override);

        EffectiveRateLimitPolicy effective = resolver.forPolicy(RateLimitPolicy.AUTH_LOGIN_ACCOUNT);

        assertThat(effective.capacity()).isEqualTo(8);
    }

    @Test
    void explicitSettingsSnapshotIsUsedInsteadOfRereadingTheSource() {
        EffectiveRateLimitPolicy effective = resolver.forPolicy(
                RateLimitPolicy.AUTH_LOGIN_ACCOUNT, new LoginRateLimitSettings(true, 2, 30));

        assertThat(effective.capacity()).isEqualTo(2);
        assertThat(effective.refillPeriod()).isEqualTo(Duration.ofMinutes(30));
    }
}