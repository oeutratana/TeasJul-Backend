package com.example.spring_boot_project_api.ratelimit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import com.example.spring_boot_project_api.service.LoginRateLimitSettings;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class RateLimitingFilterTest {

    private static final String LOGIN_BODY =
            "{\"usernameOrEmail\":\"alice@teasjul.com\",\"password\":\"supersecret123\"}";

    private final RateLimitProperties properties = new RateLimitProperties();

    /**
     * Mirrors the {@code AUTH_LOGIN_ACCOUNT} override configured in
     * {@link #setUp()} so the pre-existing expectations below stay valid; the
     * login-settings tests override it explicitly.
     */
    private LoginRateLimitSettings loginSettings = new LoginRateLimitSettings(true, 2, 15);

    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        RateLimitProperties.Policy loginIp = new RateLimitProperties.Policy();
        loginIp.setCapacity(2L);
        properties.getPolicies().put("AUTH_LOGIN_IP", loginIp);

        RateLimitProperties.Policy loginAccount = new RateLimitProperties.Policy();
        loginAccount.setCapacity(2L);
        properties.getPolicies().put("AUTH_LOGIN_ACCOUNT", loginAccount);

        RateLimitProperties.Policy resetIp = new RateLimitProperties.Policy();
        resetIp.setCapacity(1L);
        properties.getPolicies().put("AUTH_PASSWORD_RESET_IP", resetIp);

        RateLimitProperties.Policy apiIp = new RateLimitProperties.Policy();
        apiIp.setCapacity(50L);
        properties.getPolicies().put("AUTHENTICATED_API_IP", apiIp);

        RateLimitProperties.Policy apiUser = new RateLimitProperties.Policy();
        apiUser.setCapacity(2L);
        properties.getPolicies().put("AUTHENTICATED_API_USER", apiUser);

        filter = buildFilter(new CaffeineBucketStore(properties));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private RateLimitingFilter buildFilter(BucketStore store) {
        return new RateLimitingFilter(properties,
                new RateLimitRuleResolver(),
                new EffectivePolicyResolver(properties, () -> loginSettings),
                store,
                new ClientIpResolver(properties),
                new AuthAttemptIdentityExtractor(),
                () -> loginSettings);
    }

    private static MockHttpServletRequest request(String method, String path, String body, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        if (remoteAddr != null) {
            request.setRemoteAddr(remoteAddr);
        }
        if (body != null) {
            request.setContent(body.getBytes(UTF_8));
            request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        }
        return request;
    }

private static final class RecordingChain implements FilterChain {
        boolean invoked;
        HttpServletRequest lastRequest;

        @Override
        public void doFilter(jakarta.servlet.ServletRequest request,
                jakarta.servlet.ServletResponse response) {
            invoked = true;
            lastRequest = (HttpServletRequest) request;
        }
    }

    @Test
    void publicReadsProceedWhileUnderLimit() throws Exception {
        RecordingChain chain = new RecordingChain();
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("GET", "/api/public/rooms", null, null), response, chain);
        assertThat(chain.invoked).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsByteArray()).isEmpty();
    }

    @Test
    void loginIsBlockedWith429AndRetryAfterWhenIpLimitExhausted() throws Exception {
        for (int i = 0; i < 2; i++) {
            RecordingChain chain = new RecordingChain();
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request("POST", "/api/v1/auth/login", LOGIN_BODY, null), response, chain);
            assertThat(chain.invoked).as("attempt %d should pass while under limit", i + 1).isTrue();
        }

        RecordingChain chain = new RecordingChain();
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("POST", "/api/v1/auth/login", LOGIN_BODY, null), response, chain);

        assertThat(chain.invoked).isFalse();
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isNotNull();
        assertThat(Integer.parseInt(response.getHeader("Retry-After"))).isGreaterThanOrEqualTo(1);
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);

        String body = response.getContentAsString();
        assertThat(body).contains("Too many requests");
        assertThat(body).contains("/api/v1/auth/login");
        assertThat(body).contains("\"status\":429");
        assertThat(body).doesNotContain("supersecret123");
        assertThat(body).doesNotContain("alice@teasjul.com");
    }

    @Test
    void accountBucketsAreSharedAcrossIpAndRevertedPerDifferentAccount() throws Exception {
        // AUTH_LOGIN_IP is 2, so stop it from interfering by switching accounts and IPs.
        for (int i = 0; i < 2; i++) {
            makeRequestOk("POST", "/api/v1/auth/login", LOGIN_BODY, "10.1.1." + i);
        }
        String otherAccount = "{\"usernameOrEmail\":\"bob@teasjul.com\",\"password\":\"x\"}";

        // Third attempt for the same account (even from a new IP) must be denied by AUTH_LOGIN_ACCOUNT.
        MockHttpServletResponse denied = makeRequest429("POST", "/api/v1/auth/login", LOGIN_BODY, "10.1.9.9");
        assertThat(denied.getStatus()).isEqualTo(429);

        // A different account from an untouched IP still passes.
        makeRequestOk("POST", "/api/v1/auth/login", otherAccount, "10.1.8.8");
    }

    @Test
    void resetPasswordLimitIsPerIpAndOtherIpsAreUnaffected() throws Exception {
        makeRequestOk("POST", "/api/v1/auth/forgot-password",
                "{\"email\":\"a@b.com\"}", "10.0.0.1");
        MockHttpServletResponse blocked = makeRequest429("POST", "/api/v1/auth/forgot-password",
                "{\"email\":\"a@b.com\"}", "10.0.0.1");
        assertThat(blocked.getStatus()).isEqualTo(429);

        makeRequestOk("POST", "/api/v1/auth/forgot-password",
                "{\"email\":\"a@b.com\"}", "10.0.0.2");
    }

    @Test
    void paymentCallbackPassesIndependentlyOfLoginLimits() throws Exception {
        // Exhaust the login IP bucket, then show callbacks are unaffected.
        makeRequestOk("POST", "/api/v1/auth/login", LOGIN_BODY, "10.2.0.1");
        makeRequestOk("POST", "/api/v1/auth/login", LOGIN_BODY, "10.2.0.1");
        makeRequest429("POST", "/api/v1/auth/login", LOGIN_BODY, "10.2.0.1");
        for (int i = 0; i < 3; i++) {
            makeRequestOk("POST", "/api/payments/callback",
                    "{\"v1hash\":\"abc123\"}", "127.0.0.1");
        }
    }

    @Test
    void requestBodyRemainsReadableAfterFilterConsumedIt() throws Exception {
        RecordingChain chain = new RecordingChain();
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("POST", "/api/v1/auth/login", LOGIN_BODY, null), response, chain);

        assertThat(chain.invoked).isTrue();
        byte[] downstream = chain.lastRequest.getInputStream().readAllBytes();
        assertThat(new String(downstream, UTF_8)).isEqualTo(LOGIN_BODY);
    }

    @Test
    void authenticatedUserLimitsArePerPrincipalAndOthersAreUnaffected() throws Exception {
        try {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(user("owner1"), null, List.of()));
            for (int i = 0; i < 2; i++) {
                makeRequestOk("PUT", "/api/owner/properties/1", "{}", "10.3.0.1");
            }
            assertThat(makeRequest429("PUT", "/api/owner/properties/1", "{}", "10.3.0.1").getStatus())
                    .isEqualTo(429);

            SecurityContextHolder.clearContext();
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(user("owner2"), null, List.of()));
            makeRequestOk("PUT", "/api/owner/properties/1", "{}", "10.3.0.2");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void unauthenticatedRequestsSkipUserScopedChecksButHitIpCheck() throws Exception {
        // No security context: USER-scope checks are skipped, but the API_IP (50) is high enough to pass.
        for (int i = 0; i < 5; i++) {
            makeRequestOk("GET", "/api/v1/security", null, "10.4.0.1");
        }
    }

    @Test
    void failOpenBusinessPolicyLetsRequestThroughOnStoreFailure() throws Exception {
        RateLimitingFilter failing = buildFilter(new FailingStore());
        RecordingChain chain = new RecordingChain();
        MockHttpServletResponse response = new MockHttpServletResponse();
        failing.doFilter(request("GET", "/api/public/rooms", null, null), response, chain);
        assertThat(chain.invoked).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void failClosedAuthPolicyDeniesOnStoreFailure() throws Exception {
        RateLimitingFilter failing = buildFilter(new FailingStore());
        RecordingChain chain = new RecordingChain();
        MockHttpServletResponse response = new MockHttpServletResponse();
        failing.doFilter(request("POST", "/api/v1/auth/login", LOGIN_BODY, null), response, chain);
        assertThat(chain.invoked).isFalse();
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains("/api/v1/auth/login");
    }

    @Test
    void disabledSwitchDisablesAllLimiting() throws Exception {
        properties.setEnabled(false);
        for (int i = 0; i < 5; i++) {
            makeRequestOk("POST", "/api/v1/auth/login", LOGIN_BODY, null);
        }
    }

    @Test
    void loginIsNotLimitedWhenSuperAdminTurnsLoginRateLimitingOff() throws Exception {
        loginSettings = new LoginRateLimitSettings(false, 2, 15);
        for (int i = 0; i < 5; i++) {
            makeRequestOk("POST", "/api/v1/auth/login", LOGIN_BODY, "10.5.0.1");
        }
    }

    @Test
    void otherAuthSurfacesStayLimitedWhenLoginRateLimitingIsOff() throws Exception {
        loginSettings = new LoginRateLimitSettings(false, 2, 15);
        makeRequestOk("POST", "/api/v1/auth/forgot-password", "{\"email\":\"a@b.com\"}", "10.6.0.1");
        assertThat(makeRequest429("POST", "/api/v1/auth/forgot-password",
                "{\"email\":\"a@b.com\"}", "10.6.0.1").getStatus()).isEqualTo(429);
    }

    @Test
    void loginAccountLimitFollowsMaxFailedAttemptsSetting() throws Exception {
        properties.getPolicies().get("AUTH_LOGIN_IP").setCapacity(50L);
        loginSettings = new LoginRateLimitSettings(true, 3, 15);

        makeRequestOk("POST", "/api/v1/auth/login", LOGIN_BODY, "10.7.0.1");
        makeRequestOk("POST", "/api/v1/auth/login", LOGIN_BODY, "10.7.0.1");
        makeRequestOk("POST", "/api/v1/auth/login", LOGIN_BODY, "10.7.0.1");
        assertThat(makeRequest429("POST", "/api/v1/auth/login", LOGIN_BODY, "10.7.0.1").getStatus())
                .isEqualTo(429);
    }

    @Test
    void lockoutDurationSettingDrivesTheRetryAfterWindow() throws Exception {
        // Keep the per-IP guard out of the way so the account bucket is the one denying.
        properties.getPolicies().get("AUTH_LOGIN_IP").setCapacity(50L);
        loginSettings = new LoginRateLimitSettings(true, 1, 30);

        makeRequestOk("POST", "/api/v1/auth/login", LOGIN_BODY, "10.8.0.1");
        MockHttpServletResponse blocked = makeRequest429("POST", "/api/v1/auth/login", LOGIN_BODY, "10.8.0.1");

        assertThat(Long.parseLong(blocked.getHeader("Retry-After"))).isBetween(1700L, 1800L);
    }

    @Test
    void limiterConsumesTheConfiguredAttemptsAndLockoutWindow() throws Exception {
        properties.getPolicies().get("AUTH_LOGIN_IP").setCapacity(20L);
        loginSettings = new LoginRateLimitSettings(true, 4, 30);
        RecordingStore store = new RecordingStore();
        RateLimitingFilter recording = buildFilter(store);

        RecordingChain chain = new RecordingChain();
        recording.doFilter(request("POST", "/api/v1/auth/login", LOGIN_BODY, "10.9.0.1"),
                new MockHttpServletResponse(), chain);

        assertThat(chain.invoked).isTrue();
        assertThat(store.lastCallFor("auth_login_account")).isEqualTo(new long[] {4L, 1_800L});
        assertThat(store.lastCallFor("auth_login_ip")).isEqualTo(new long[] {20L, 900L});
    }

    @Test
    void accountIsUsableAgainAfterTheConfiguredLockoutWindowElapses() throws Exception {
        properties.getPolicies().get("AUTH_LOGIN_IP").setCapacity(50L);
        loginSettings = new LoginRateLimitSettings(true, 1, 15);
        WindowStore store = new WindowStore();
        RateLimitingFilter windowed = buildFilter(store);

        RecordingChain first = new RecordingChain();
        windowed.doFilter(request("POST", "/api/v1/auth/login", LOGIN_BODY, "10.10.0.1"),
                new MockHttpServletResponse(), first);
        assertThat(first.invoked).isTrue();

        RecordingChain locked = new RecordingChain();
        MockHttpServletResponse denied = new MockHttpServletResponse();
        windowed.doFilter(request("POST", "/api/v1/auth/login", LOGIN_BODY, "10.10.0.1"), denied, locked);
        assertThat(locked.invoked).isFalse();
        assertThat(denied.getStatus()).isEqualTo(429);

        store.advanceMinutes(15);

        RecordingChain afterWindow = new RecordingChain();
        MockHttpServletResponse allowed = new MockHttpServletResponse();
        windowed.doFilter(request("POST", "/api/v1/auth/login", LOGIN_BODY, "10.10.0.1"), allowed, afterWindow);
        assertThat(afterWindow.invoked).isTrue();
        assertThat(allowed.getStatus()).isEqualTo(200);
    }

    private void makeRequestOk(String method, String path, String body, String remoteAddr)
            throws IOException, jakarta.servlet.ServletException {
        RecordingChain chain = new RecordingChain();
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request(method, path, body, remoteAddr), response, chain);
        assertThat(chain.invoked)
                .as("request %s %s should be allowed", method, path)
                .isTrue();
    }

    private MockHttpServletResponse makeRequest429(String method, String path, String body, String remoteAddr)
            throws IOException, jakarta.servlet.ServletException {
        RecordingChain chain = new RecordingChain();
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request(method, path, body, remoteAddr), response, chain);
        assertThat(chain.invoked)
                .as("request %s %s should be blocked", method, path)
                .isFalse();
        assertThat(response.getStatus()).isEqualTo(429);
        return response;
    }

    private static User user(String username) {
        return new User(username, "dummy", List.of());
    }

    private static final class FailingStore implements BucketStore {
        @Override
        public BucketResult tryConsume(String key, long capacity, java.time.Duration refillPeriod) {
            throw new RuntimeException("simulated store outage");
        }
    }

    /** Allows everything, but remembers the enforcement parameters per bucket namespace. */
    private static final class RecordingStore implements BucketStore {
        private final java.util.Map<String, long[]> calls = new java.util.LinkedHashMap<>();

        @Override
        public BucketResult tryConsume(String key, long capacity, java.time.Duration refillPeriod) {
            calls.put(bucketNamespace(key), new long[] {capacity, refillPeriod.toSeconds()});
            return BucketResult.allow();
        }

        long[] lastCallFor(String namespace) {
            long[] recorded = calls.get(namespace);
            assertThat(recorded).as("bucket namespace %s should have been used", namespace).isNotNull();
            return recorded;
        }

        private static String bucketNamespace(String key) {
            int separator = key.indexOf(':');
            return separator < 0 ? key : key.substring(0, separator);
        }
    }

    /**
     * Deterministic fixed-window stand-in for the token bucket so the configured
     * lockout duration can be observed without waiting for real time to pass.
     */
    private static final class WindowStore implements BucketStore {
        private final java.util.Map<String, long[]> windows = new java.util.HashMap<>();
        private long nowMillis = 0L;

        void advanceMinutes(long minutes) {
            nowMillis += java.time.Duration.ofMinutes(minutes).toMillis();
        }

        @Override
        public BucketResult tryConsume(String key, long capacity, java.time.Duration refillPeriod) {
            long[] window = windows.get(key);
            if (window == null) {
                window = new long[] {nowMillis, 0L};
                windows.put(key, window);
            }
            long refillMillis = refillPeriod.toMillis();
            if (nowMillis - window[0] >= refillMillis) {
                window[0] = nowMillis;
                window[1] = 0L;
            }
            window[1]++;
            return window[1] <= capacity
                    ? BucketResult.allow()
                    : BucketResult.deny(java.time.Duration.ofMillis(refillMillis - (nowMillis - window[0])));
        }
    }
}