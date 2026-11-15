package com.example.spring_boot_project_api.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import com.example.spring_boot_project_api.util.SecurityCodeUtil;

class AuthAttemptIdentityExtractorTest {

    private final AuthAttemptIdentityExtractor extractor =
            new AuthAttemptIdentityExtractor();

    @Test
    void extractsAndHashesLoginIdentifier() {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/v1/auth/login");
        byte[] body = "{\"usernameOrEmail\":\"Admin@TeasJul.com\",\"password\":\"s3cret\"}"
                .getBytes(StandardCharsets.UTF_8);

        assertThat(extractor.extractAccountKey(request, body))
                .contains(SecurityCodeUtil.hashSecret("admin@teasjul.com"));
    }

    @Test
    void extractsEmailForForgotPassword() {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/v1/auth/forgot-password");
        byte[] body = "{\"email\":\" tenant@example.com \"}".getBytes(StandardCharsets.UTF_8);

        assertThat(extractor.extractAccountKey(request, body))
                .contains(SecurityCodeUtil.hashSecret("tenant@example.com"));
    }

    @Test
    void usesChallengeTokenAsAccountKeyFor2faVerify() {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/v1/auth/2fa/verify");
        byte[] body = "{\"challengeToken\":\"ChAlLeNgE-123\",\"otp\":\"111111\"}"
                .getBytes(StandardCharsets.UTF_8);

        assertThat(extractor.extractAccountKey(request, body))
                .contains(SecurityCodeUtil.hashSecret("challenge-123"));
    }

    @Test
    void returnsEmptyWhenFieldMissingOrBlank() {
        MockHttpServletRequest login = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        assertThat(extractor.extractAccountKey(login, "{}".getBytes(StandardCharsets.UTF_8)))
                .isEmpty();
        assertThat(extractor.extractAccountKey(login,
                "{\"usernameOrEmail\":\"   \"}".getBytes(StandardCharsets.UTF_8)))
                .isEmpty();
    }

    @Test
    void returnsEmptyForUnparseableOrEmptyBody() {
        MockHttpServletRequest login = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        assertThat(extractor.extractAccountKey(login, "not json".getBytes(StandardCharsets.UTF_8)))
                .isEmpty();
        assertThat(extractor.extractAccountKey(login, new byte[0])).isEmpty();
        assertThat(extractor.extractAccountKey(login, null)).isEmpty();
    }

    @Test
    void resetPasswordHasNoAccountIdentifierAndIsNeverExtracted() {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/v1/auth/reset-password");
        byte[] body = "{\"resetToken\":\"tok-1\",\"newPassword\":\"x\"}"
                .getBytes(StandardCharsets.UTF_8);

        assertThat(extractor.extractAccountKey(request, body)).isEmpty();
    }

    @Test
    void getRequestsNeverReturnAField() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/public/rooms");
        assertThat(extractor.fieldFor(request)).isNull();
    }
}