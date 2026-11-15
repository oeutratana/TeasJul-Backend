package com.example.spring_boot_project_api.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the fail-closed JWT secret handling introduced to remove the
 * hardcoded signing-secret fallback (finding F-01).
 *
 * <p>Secrets used here are obviously synthetic test placeholders and are never
 * the production value.</p>
 */
class JwtUtilTest {

    private static final String VALID_SECRET_A =
            "test-only-secret-key-AAAAAAAAAAAAAAAAAAAAAAAA";
    private static final String VALID_SECRET_B =
            "test-only-secret-key-BBBBBBBBBBBBBBBBBBBBBBBB";
    private static final long ONE_HOUR_MS = 3_600_000L;

    @Test
    void constructorRejectsNullSecret() {
        assertThatThrownBy(() -> new JwtUtil(null, ONE_HOUR_MS))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void constructorRejectsBlankSecret() {
        assertThatThrownBy(() -> new JwtUtil("", ONE_HOUR_MS))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtUtil("     ", ONE_HOUR_MS))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void constructorRejectsSecretShorterThan256Bits() {
        assertThatThrownBy(() -> new JwtUtil("too-short-secret", ONE_HOUR_MS))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void generateAndValidateRoundTripWithValidSecret() {
        JwtUtil jwtUtil = new JwtUtil(VALID_SECRET_A, ONE_HOUR_MS);

        String token = jwtUtil.generateToken("alice");

        assertThat(jwtUtil.isTokenValid(token)).isTrue();
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("alice");
        assertThat(jwtUtil.getExpirationMs()).isEqualTo(ONE_HOUR_MS);
    }

    /**
     * A token minted with one key must not validate under a different configured
     * key. This demonstrates that a token signed with any previously configured
     * key (such as a removed fallback) is rejected once a different key is used.
     */
    @Test
    void tokenSignedWithDifferentKeyIsRejected() {
        JwtUtil signer = new JwtUtil(VALID_SECRET_A, ONE_HOUR_MS);
        JwtUtil verifier = new JwtUtil(VALID_SECRET_B, ONE_HOUR_MS);

        String token = signer.generateToken("alice");

        assertThat(verifier.isTokenValid(token)).isFalse();
    }

    @Test
    void expiredTokenIsInvalid() {
        JwtUtil jwtUtil = new JwtUtil(VALID_SECRET_A, -1_000L);

        String token = jwtUtil.generateToken("bob");

        assertThat(jwtUtil.isTokenValid(token)).isFalse();
    }
}
