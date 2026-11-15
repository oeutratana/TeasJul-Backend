package com.example.spring_boot_project_api.service.impl;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * In-memory, single-use store for short-lived security tokens (password-reset
 * authorization and 2FA challenge tokens). Only the SHA-256 hash of a token is
 * ever kept, and each token can be validated at most once. TTL is enforced on
 * lookup. Suitable for a single application instance.
 */
@Component
public class OneTimeTokenStore {

    private final Map<String, Entry> tokens = new ConcurrentHashMap<>();

    public void put(String rawToken, String purpose, Long userId, Instant expiresAt) {
        tokens.put(hash(rawToken), new Entry(purpose, userId, expiresAt));
    }

    /**
     * Validates and, if valid, atomically consumes the token. Returns the user id
     * when the token is present, unexpired, single-use and matches the purpose,
     * otherwise {@code null}.
     */
    public Long consume(String rawToken, String purpose) {
        String key = hash(rawToken);
        Entry entry = tokens.remove(key);
        if (entry == null) {
            return null;
        }
        if (!entry.purpose.equals(purpose) || entry.expiresAt.isBefore(Instant.now())) {
            return null;
        }
        return entry.userId;
    }

    /** Invalidates a stored token (used when invalidating outstanding tokens). */
    public void invalidate(String purpose, Long userId) {
        tokens.entrySet().removeIf(e ->
                e.getValue().purpose.equals(purpose) && e.getValue().userId.equals(userId));
    }

    private static String hash(String raw) {
        return com.example.spring_boot_project_api.util.SecurityCodeUtil.hashSecret(raw);
    }

    private record Entry(String purpose, Long userId, Instant expiresAt) {
    }
}
