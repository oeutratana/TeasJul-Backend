package com.example.spring_boot_project_api.ratelimit;

import java.io.IOException;

import com.example.spring_boot_project_api.util.SecurityCodeUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Derives a stable, non-sensitive account identifier from the request body of
 * authentication / account-security endpoints. The identifier is only ever used
 * as a (hashed) bucket key — its raw value is never logged or persisted, and
 * secrets such as OTP codes are never read as identities.
 *
 * <p>See the {@code fieldFor} mapping below for which body field identifies the
 * account for each endpoint; {@code reset-password} deliberately has none (its
 * token is per-request and not guessable, so IP-only limiting applies).
 */
public class AuthAttemptIdentityExtractor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Returns the SHA-256 (hex) key of the account identifier found in the
     * request body for this endpoint, or empty when none can be determined
     * (missing body, unparseable JSON, missing field, or no identity concept).
     */
    public java.util.Optional<String> extractAccountKey(HttpServletRequest request, byte[] body) {
        String field = fieldFor(request);
        if (field == null || body == null || body.length == 0) {
            return java.util.Optional.empty();
        }
        JsonNode root;
        try {
            root = OBJECT_MAPPER.readTree(body);
        } catch (IOException ex) {
            return java.util.Optional.empty();
        }
        if (root == null || !root.isObject()) {
            return java.util.Optional.empty();
        }
        JsonNode node = root.get(field);
        if (node == null || node.isNull() || !node.isTextual()) {
            return java.util.Optional.empty();
        }
        String value = node.asText().trim().toLowerCase();
        return value.isEmpty() ? java.util.Optional.empty()
                : java.util.Optional.of(SecurityCodeUtil.hashSecret(value));
    }

    /** Optional replayable (idempotent) dispatcher, kept for unit-testability. */
    String fieldFor(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod())) {
            return null;
        }
        String path = request.getRequestURI();
        return switch (path) {
            case "/api/v1/auth/login" -> "usernameOrEmail";
            case "/api/v1/auth/forgot-password" -> "email";
            case "/api/v1/auth/forgot-password/verify-otp" -> "email";
            case "/api/v1/auth/2fa/verify" -> "challengeToken";
            case "/api/v1/security/recovery-email" -> "recoveryEmail";
            case "/api/v1/security/recovery-email/verify" -> "recoveryEmail";
            default -> null;
        };
    }
}