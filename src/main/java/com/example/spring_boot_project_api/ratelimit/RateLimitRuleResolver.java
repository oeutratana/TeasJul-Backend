package com.example.spring_boot_project_api.ratelimit;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Maps an incoming request to the ordered list of rate-limit checks that apply
 * to it. Resolution is purely structural (method + path), so it never touches
 * the database or security context; identity for {@link CheckScope#ACCOUNT}
 * checks is supplied later by {@link AuthAttemptIdentityExtractor}.
 */
public class RateLimitRuleResolver {

    /** Result of route classification. */
    public enum CheckScope {
        /** Check keyed on the resolved client IP. */
        IP,
        /** Check keyed on the (hashed) account identifier from the request body. */
        ACCOUNT,
        /** Check keyed on the (hashed) authenticated username. */
        USER
    }

    /** A single check applied during request processing. */
    public record RateLimitCheck(RateLimitPolicy policy, CheckScope scope) {
    }

    private static final List<RateLimitCheck> EMPTY = List.of();

    private static final RateLimitCheck LOGIN_IP = ip(RateLimitPolicy.AUTH_LOGIN_IP);
    private static final RateLimitCheck LOGIN_ACCOUNT = account(RateLimitPolicy.AUTH_LOGIN_ACCOUNT);
    private static final RateLimitCheck REGISTER_IP = ip(RateLimitPolicy.AUTH_REGISTER_IP);
    private static final RateLimitCheck PASSWORD_RESET_IP = ip(RateLimitPolicy.AUTH_PASSWORD_RESET_IP);
    private static final RateLimitCheck PASSWORD_RESET_ACCOUNT = account(RateLimitPolicy.AUTH_PASSWORD_RESET_ACCOUNT);
    private static final RateLimitCheck OTP_IP = ip(RateLimitPolicy.AUTH_OTP_IP);
    private static final RateLimitCheck OTP_ACCOUNT = account(RateLimitPolicy.AUTH_OTP_ACCOUNT);
    private static final RateLimitCheck TWO_FA_IP = ip(RateLimitPolicy.AUTH_2FA_IP);
    private static final RateLimitCheck TWO_FA_ACCOUNT = account(RateLimitPolicy.AUTH_2FA_ACCOUNT);
    private static final RateLimitCheck READ_IP = ip(RateLimitPolicy.PUBLIC_READ);
    private static final RateLimitCheck DETAIL_IP = ip(RateLimitPolicy.PUBLIC_DETAIL);
    private static final RateLimitCheck CONTACT_IP = ip(RateLimitPolicy.PUBLIC_CONTACT);
    private static final RateLimitCheck API_IP = ip(RateLimitPolicy.AUTHENTICATED_API_IP);
    private static final RateLimitCheck API_USER = user(RateLimitPolicy.AUTHENTICATED_API_USER);
    private static final RateLimitCheck SENSITIVE_USER = user(RateLimitPolicy.SENSITIVE_WRITE_USER);
    private static final RateLimitCheck CALLBACK_IP = ip(RateLimitPolicy.PAYMENT_CALLBACK);

    /**
     * @return checks to apply, in evaluation order
     */
    public List<RateLimitCheck> resolve(HttpServletRequest request) {
        if ("OPTIONS".equals(request.getMethod())) {
            return EMPTY;
        }
        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            return EMPTY;
        }

        return switch (request.getMethod()) {
            case "GET" -> resolveGet(path);
            case "POST" -> resolvePost(path);
            default -> authenticatedApi();
        };
    }

    private List<RateLimitCheck> resolveGet(String path) {
        if (path.startsWith("/api/public/")) {
            return isDetailPath(path) ? List.of(DETAIL_IP) : List.of(READ_IP);
        }
        if (path.equals("/api/v1/tenant-invitations/validate")) {
            return List.of(READ_IP);
        }
        return authenticatedApi();
    }

    private List<RateLimitCheck> resolvePost(String path) {
        switch (path) {
            case "/api/v1/auth/login":
                return List.of(LOGIN_IP, LOGIN_ACCOUNT);
            case "/api/v1/auth/register":
                return List.of(REGISTER_IP);
            case "/api/v1/auth/forgot-password":
                return List.of(PASSWORD_RESET_IP, PASSWORD_RESET_ACCOUNT);
            case "/api/v1/auth/forgot-password/verify-otp":
                return List.of(OTP_IP, OTP_ACCOUNT);
            case "/api/v1/auth/reset-password":
                return List.of(PASSWORD_RESET_IP);
            case "/api/v1/auth/2fa/verify":
                return List.of(TWO_FA_IP, TWO_FA_ACCOUNT);
            case "/api/v1/security/recovery-email":
            case "/api/v1/security/recovery-email/verify":
                return List.of(OTP_IP, OTP_ACCOUNT);
            case "/api/payments/callback":
                return List.of(CALLBACK_IP);
            case "/api/payments":
            case "/api/payments/manual":
            case "/api/owner/feedback":
            case "/api/v1/tenant-invitations":
                return sensitive();
            case "/api/v1/tenant-invitations/consume":
                return List.of(CONTACT_IP);
            default:
                return authenticatedApi();
        }
    }

    private List<RateLimitCheck> sensitive() {
        return List.of(API_IP, API_USER, SENSITIVE_USER);
    }

    private List<RateLimitCheck> authenticatedApi() {
        return List.of(API_IP, API_USER);
    }

    private static boolean isDetailPath(String path) {
        return path.startsWith("/api/public/listings/")
                || (path.startsWith("/api/public/rooms/") && path.length() > "/api/public/rooms/".length());
    }

    private static RateLimitCheck ip(RateLimitPolicy policy) {
        return new RateLimitCheck(policy, CheckScope.IP);
    }

    private static RateLimitCheck account(RateLimitPolicy policy) {
        return new RateLimitCheck(policy, CheckScope.ACCOUNT);
    }

    private static RateLimitCheck user(RateLimitPolicy policy) {
        return new RateLimitCheck(policy, CheckScope.USER);
    }
}