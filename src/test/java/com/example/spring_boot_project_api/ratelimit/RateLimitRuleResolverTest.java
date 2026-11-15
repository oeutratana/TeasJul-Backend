package com.example.spring_boot_project_api.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import com.example.spring_boot_project_api.ratelimit.RateLimitRuleResolver.CheckScope;
import com.example.spring_boot_project_api.ratelimit.RateLimitRuleResolver.RateLimitCheck;

class RateLimitRuleResolverTest {

    private final RateLimitRuleResolver resolver = new RateLimitRuleResolver();

    private static MockHttpServletRequest request(String method, String path) {
        return new MockHttpServletRequest(method, path);
    }

    private static RateLimitPolicy policyOf(List<RateLimitCheck> checks, CheckScope scope) {
        return checks.stream()
                .filter(c -> c.scope() == scope)
                .map(RateLimitCheck::policy)
                .findFirst()
                .orElse(null);
    }

    @Test
    void loginIsLimitedByIpAndAccount() {
        List<RateLimitCheck> checks = resolver.resolve(request("POST", "/api/v1/auth/login"));
        assertThat(checks).hasSize(2);
        assertThat(policyOf(checks, CheckScope.IP)).isEqualTo(RateLimitPolicy.AUTH_LOGIN_IP);
        assertThat(policyOf(checks, CheckScope.ACCOUNT))
                .isEqualTo(RateLimitPolicy.AUTH_LOGIN_ACCOUNT);
    }

    @Test
    void registerIsIpLimitedOnly() {
        List<RateLimitCheck> checks = resolver.resolve(request("POST", "/api/v1/auth/register"));
        assertThat(checks).hasSize(1);
        assertThat(checks.getFirst().policy()).isEqualTo(RateLimitPolicy.AUTH_REGISTER_IP);
        assertThat(checks.getFirst().scope()).isEqualTo(CheckScope.IP);
    }

    @Test
    void publicListingUsesDetailLimit() {
        for (String path : List.of(
                "/api/public/rooms/1", "/api/public/rooms/1/similar", "/api/public/listings/42")) {
            List<RateLimitCheck> checks = resolver.resolve(request("GET", path));
            assertThat(checks).hasSize(1);
            assertThat(checks.getFirst().policy()).as("path %s", path)
                    .isEqualTo(RateLimitPolicy.PUBLIC_DETAIL);
        }
    }

    @Test
    void publicCollectionsUseReadLimit() {
        for (String path : List.of(
                "/api/public/rooms", "/api/public/room-types", "/api/public/feedback")) {
            List<RateLimitCheck> checks = resolver.resolve(request("GET", path));
            assertThat(checks).hasSize(1);
            assertThat(checks.getFirst().policy()).as("path %s", path)
                    .isEqualTo(RateLimitPolicy.PUBLIC_READ);
        }
        List<RateLimitCheck> validate =
                resolver.resolve(request("GET", "/api/v1/tenant-invitations/validate"));
        assertThat(validate.getFirst().policy()).isEqualTo(RateLimitPolicy.PUBLIC_READ);
    }

    @Test
    void paymentCallbackGetsItsOwnGenerousPolicy() {
        List<RateLimitCheck> checks = resolver.resolve(request("POST", "/api/payments/callback"));
        assertThat(checks).hasSize(1);
        assertThat(checks.getFirst().policy()).isEqualTo(RateLimitPolicy.PAYMENT_CALLBACK);
        assertThat(RateLimitPolicy.PAYMENT_CALLBACK.getCapacity())
                .isGreaterThan(RateLimitPolicy.SENSITIVE_WRITE_USER.getCapacity());
    }

    @Test
    void sensitiveWritesGetAuthenticatedAndStrictWriteLimits() {
        for (String path : List.of(
                "/api/payments", "/api/payments/manual",
                "/api/owner/feedback", "/api/v1/tenant-invitations")) {
            List<RateLimitCheck> checks = resolver.resolve(request("POST", path));
            assertThat(checks).as("path %s", path).hasSize(3);
            assertThat(policyOf(checks, CheckScope.IP))
                    .isEqualTo(RateLimitPolicy.AUTHENTICATED_API_IP);
            assertThat(policyOf(checks, CheckScope.USER))
                    .isEqualTo(RateLimitPolicy.AUTHENTICATED_API_USER);
            assertThat(checks).as("path %s", path)
                    .anyMatch(c -> c.scope() == CheckScope.USER
                            && c.policy() == RateLimitPolicy.SENSITIVE_WRITE_USER);
        }
    }

    @Test
    void consumeInvitationUsesContactLimit() {
        List<RateLimitCheck> checks =
                resolver.resolve(request("POST", "/api/v1/tenant-invitations/consume"));
        assertThat(checks).hasSize(1);
        assertThat(checks.getFirst().policy()).isEqualTo(RateLimitPolicy.PUBLIC_CONTACT);
    }

    @Test
    void authenticatedApiDefaultsApplyToOtherApiRoutes() {
        List<RateLimitCheck> checks = resolver.resolve(request("PUT", "/api/admin/rooms/1"));
        assertThat(policyOf(checks, CheckScope.IP)).isEqualTo(RateLimitPolicy.AUTHENTICATED_API_IP);
        assertThat(policyOf(checks, CheckScope.USER)).isEqualTo(RateLimitPolicy.AUTHENTICATED_API_USER);
    }

    @Test
    void optionsAndNonApiAreExempt() {
        assertThat(resolver.resolve(request("OPTIONS", "/api/public/rooms"))).isEmpty();
        assertThat(resolver.resolve(request("GET", "/index.html"))).isEmpty();
        assertThat(resolver.resolve(request("GET", "/error"))).isEmpty();
        assertThat(resolver.resolve(request("GET", "/swagger-ui.html"))).isEmpty();
        assertThat(resolver.resolve(request("GET", "/v3/api-docs"))).isEmpty();
    }
}