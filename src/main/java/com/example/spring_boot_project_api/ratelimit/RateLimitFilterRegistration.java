package com.example.spring_boot_project_api.ratelimit;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.spring_boot_project_api.service.SystemSettingService;

/**
 * Owns every bean of the rate-limiting engine and registers
 * {@link RateLimitingFilter} as a plain servlet filter running <em>after</em>
 * the Spring Security filter chain (order > -100), matched on {@code /*}.
 *
 * <p>The beans are deliberately <b>not</b> {@code @Component}-annotated and live
 * only in this {@code @Configuration} class, so {@code @WebMvcTest} slice tests
 * (which never scan this class) remain untouched while rate limiting is active
 * in production and full-context integration tests.
 */
@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitFilterRegistration {

    @Bean
    public LoginRateLimitSettingsSource loginRateLimitSettingsSource(SystemSettingService systemSettingService) {
        return new SystemSettingLoginRateLimitSettingsSource(systemSettingService);
    }

    @Bean
    public EffectivePolicyResolver effectivePolicyResolver(RateLimitProperties rateLimitProperties,
            LoginRateLimitSettingsSource loginRateLimitSettingsSource) {
        return new EffectivePolicyResolver(rateLimitProperties, loginRateLimitSettingsSource);
    }

    @Bean
    public BucketStore rateLimitBucketStore(RateLimitProperties rateLimitProperties) {
        return new CaffeineBucketStore(rateLimitProperties);
    }

    @Bean
    public ClientIpResolver clientIpResolver(RateLimitProperties rateLimitProperties) {
        return new ClientIpResolver(rateLimitProperties);
    }

    @Bean
    public AuthAttemptIdentityExtractor authAttemptIdentityExtractor() {
        return new AuthAttemptIdentityExtractor();
    }

    @Bean
    public RateLimitRuleResolver rateLimitRuleResolver() {
        return new RateLimitRuleResolver();
    }

    @Bean
    public RateLimitingFilter rateLimitingFilter(RateLimitProperties rateLimitProperties,
            RateLimitRuleResolver rateLimitRuleResolver,
            EffectivePolicyResolver effectivePolicyResolver,
            BucketStore rateLimitBucketStore,
            ClientIpResolver clientIpResolver,
            AuthAttemptIdentityExtractor authAttemptIdentityExtractor,
            LoginRateLimitSettingsSource loginRateLimitSettingsSource) {
        return new RateLimitingFilter(rateLimitProperties, rateLimitRuleResolver,
                effectivePolicyResolver, rateLimitBucketStore, clientIpResolver,
                authAttemptIdentityExtractor, loginRateLimitSettingsSource);
    }

    /**
     * Explicit registration (order 0 = right after the security chain at -100)
     * so the filter is guaranteed a single wiring point and is not
     * auto-registered a second time by Spring Boot's servlet filter discovery.
     */
    @Bean
    public FilterRegistrationBean<RateLimitingFilter> rateLimitingFilterRegistration(
            RateLimitingFilter rateLimitingFilter) {
        FilterRegistrationBean<RateLimitingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(rateLimitingFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(0);
        registration.setEnabled(true);
        return registration;
    }
}