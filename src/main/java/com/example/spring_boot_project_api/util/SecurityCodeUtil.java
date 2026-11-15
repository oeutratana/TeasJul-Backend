package com.example.spring_boot_project_api.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Stateless helpers for generating and hashing OTP codes and short-lived
 * security tokens (password-reset authorization and 2FA challenge tokens).
 *
 * <p>Only the cryptographically secure hash of any secret is ever persisted
 * or logged; the raw value is returned to the caller exactly once so it can be
 * delivered and then discarded.
 */
public final class SecurityCodeUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private SecurityCodeUtil() {
    }

    /** Generates a 6-digit OTP using a cryptographically secure PRNG. */
    public static String generateOtp() {
        int code = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", code);
    }

    /**
     * Generates a raw security token (password-reset / challenge) from secure
     * random bytes, Base64URL encoded without padding so it is URL safe.
     */
    public static String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 hex digest of the given raw secret; only this value is stored. */
    public static String hashSecret(String rawSecret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawSecret.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }

    /** Constant-time comparison to avoid timing side channels. */
    public static boolean secureEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }
}
