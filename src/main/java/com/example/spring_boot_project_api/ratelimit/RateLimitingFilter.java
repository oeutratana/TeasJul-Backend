package com.example.spring_boot_project_api.ratelimit;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.spring_boot_project_api.dto.response.ErrorResponse;
import com.example.spring_boot_project_api.ratelimit.RateLimitRuleResolver.CheckScope;
import com.example.spring_boot_project_api.ratelimit.RateLimitRuleResolver.RateLimitCheck;
import com.example.spring_boot_project_api.service.LoginRateLimitSettings;
import com.example.spring_boot_project_api.util.SecurityCodeUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Applies the configured {@link RateLimitPolicy rate-limit policies} to inbound
 * requests before they reach a controller method.
 *
 * <p>The filter is registered as a plain servlet filter <em>after</em> the
 * Spring Security filter chain (see {@link RateLimitFilterRegistration}), so by
 * the time it runs:
 * <ul>
 *   <li>only requests that already passed spring security (permitAll or
 *       authenticated) are visible — rejected 401/403 requests never reach it;</li>
 *   <li>the {@link SecurityContextHolder} still holds the authenticated user, so
 *       per-user limits can key off the principal username without a DB hit;</li>
 *   <li>authentication endpoints (login, register, password reset, 2FA, OTP) are
 *       throttled <em>before</em> Bcrypt/OtpService work happens.</li>
 * </ul>
 *
 * <p>Fail-safe semantics per policy: {@code failClosed} policies return a 503 on
 * store failure (protect the auth surface at any cost); {@code failOpen}
 * policies let the request through and log the error (keep the product up even
 * if the limiter hiccups). Nothing secret is ever logged.
 */
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("RATE_LIMIT");

    /**
     * Self-managed mapper (the app has no Spring-managed
     * {@code com.fasterxml.jackson} {@code ObjectMapper}; {@code ErrorResponse}
     * carries an ISO-8601 timestamp, so the {@code JavaTimeModule} is required).
     */
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    private static final String TOO_MANY_MESSAGE = "Too many requests. Please try again later.";
    private static final String STORE_FAILED_MESSAGE = "Temporarily unavailable. Please try again later.";

    private final RateLimitProperties properties;
    private final RateLimitRuleResolver resolver;
    private final EffectivePolicyResolver policyResolver;
    private final BucketStore bucketStore;
    private final ClientIpResolver clientIpResolver;
    private final AuthAttemptIdentityExtractor identityExtractor;
    private final LoginRateLimitSettingsSource loginSettingsSource;

    public RateLimitingFilter(RateLimitProperties properties,
            RateLimitRuleResolver resolver,
            EffectivePolicyResolver policyResolver,
            BucketStore bucketStore,
            ClientIpResolver clientIpResolver,
            AuthAttemptIdentityExtractor identityExtractor,
            LoginRateLimitSettingsSource loginSettingsSource) {
        this.properties = properties;
        this.resolver = resolver;
        this.policyResolver = policyResolver;
        this.bucketStore = bucketStore;
        this.clientIpResolver = clientIpResolver;
        this.identityExtractor = identityExtractor;
        this.loginSettingsSource = loginSettingsSource;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        DispatcherType type = request.getDispatcherType();
        return type == DispatcherType.ERROR || type == DispatcherType.ASYNC
                || type == DispatcherType.FORWARD;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        List<RateLimitCheck> checks = resolver.resolve(request);
        if (checks.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        LoginRateLimitSettings loginSettings = loginSettingsSource.current();
        checks = withoutDisabledLoginChecks(checks, loginSettings);
        if (checks.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean needsBody = checks.stream().anyMatch(c -> c.scope() == CheckScope.ACCOUNT);
        byte[] body = needsBody ? request.getInputStream().readAllBytes() : null;
        HttpServletRequest toProcess = needsBody ? new CachedBodyHttpServletRequest(request, body) : request;

        String ip = clientIpResolver.resolve(request);
        String accountKey = needsBody ? accountKey(toProcess, body) : null;
        String userKey = userKey();

        for (RateLimitCheck check : checks) {
            EffectiveRateLimitPolicy effective = policyResolver.forPolicy(check.policy(), loginSettings);
            String key = bucketKey(check, effective.policy().name(), ip, accountKey, userKey);
            if (key == null) {
                continue;
            }
            BucketResult result;
            try {
                result = bucketStore.tryConsume(key, effective.capacity(), effective.refillPeriod());
            } catch (RuntimeException ex) {
                if (effective.failClosed()) {
                    log.error("rate-limit store failure (fail-closed, denying) policy={} method={} path={} ip={}: {}",
                            effective.policy(), request.getMethod(), request.getRequestURI(), ip, ex.getMessage());
                    write(response, HttpStatus.SERVICE_UNAVAILABLE, STORE_FAILED_MESSAGE, request.getRequestURI(), null);
                    return;
                }
                log.error("rate-limit store failure (fail-open, allowing) policy={} method={} path={} ip={}: {}",
                        effective.policy(), request.getMethod(), request.getRequestURI(), ip, ex.getMessage());
                continue;
            }

            if (!result.allowed()) {
                log.warn("rate-limited policy={} method={} path={} ip={}",
                        effective.policy(), request.getMethod(), request.getRequestURI(), ip);
                write(response, HttpStatus.TOO_MANY_REQUESTS, TOO_MANY_MESSAGE,
                        request.getRequestURI(), retainAfterSeconds(result.retryAfter()));
                return;
            }
        }

        filterChain.doFilter(toProcess, response);
    }

    /**
     * Drops the login policies when SUPER_ADMIN has switched login rate limiting
     * off. No other policy is touched, so OTP, 2FA, password reset and the
     * authenticated API keep their existing behaviour.
     */
    private List<RateLimitCheck> withoutDisabledLoginChecks(List<RateLimitCheck> checks,
            LoginRateLimitSettings loginSettings) {
        if (loginSettings == null || loginSettings.enabled()) {
            return checks;
        }
        List<RateLimitCheck> kept = checks.stream()
                .filter(check -> !LoginRateLimitSettingsSource.isLoginPolicy(check.policy()))
                .toList();
        if (kept.size() != checks.size()) {
            log.info("login rate limiting disabled by settings; skipping {} login check(s)",
                    checks.size() - kept.size());
        }
        return kept;
    }

    /**
     * Namespaced bucket key for a check. Scope identities that are unavailable
     * (no account key from body, no authenticated user) yield {@code null} and
     * the check is skipped.
     */
    private String bucketKey(RateLimitCheck check, String policyName, String ip,
            String accountKey, String userKey) {
        String prefix = policyName.toLowerCase();
        return switch (check.scope()) {
            case IP -> prefix + ":ip:" + ip;
            case ACCOUNT -> accountKey == null ? null : prefix + ":acct:" + accountKey;
            case USER -> userKey == null ? null : prefix + ":user:" + userKey;
        };
    }

    /** Hashed account identifier for ACCOUNT-scope checks (never exposed as plain text). */
    private String accountKey(HttpServletRequest toProcess, byte[] body) {
        Optional<String> key = identityExtractor.extractAccountKey(toProcess, body);
        return key.orElse(null);
    }

    /** Hashed username of the currently authenticated principal, if any. */
    private String userKey() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserDetails userDetails)) {
            return null;
        }
        String username = userDetails.getUsername();
        return username == null ? null : SecurityCodeUtil.hashSecret(username.trim().toLowerCase());
    }

    private static Duration retainAfterSeconds(Duration retryAfter) {
        long seconds = retryAfter == null ? 1 : Math.max(1, retryAfter.toSeconds());
        return Duration.ofSeconds(seconds);
    }

    private void write(HttpServletResponse response, HttpStatus status, String message, String path,
            Duration retryAfter) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        if (retryAfter != null) {
            response.setHeader("Retry-After", String.valueOf(retryAfter.toSeconds()));
        }
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        OBJECT_MAPPER.writeValue(response.getWriter(),
                ErrorResponse.of(status.value(), status.getReasonPhrase(), message, path));
    }
}