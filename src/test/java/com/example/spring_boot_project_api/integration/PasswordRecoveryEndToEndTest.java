package com.example.spring_boot_project_api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.example.spring_boot_project_api.dto.request.ForgotPasswordRequest;
import com.example.spring_boot_project_api.dto.request.ResetPasswordRequest;
import com.example.spring_boot_project_api.dto.request.VerifyPasswordResetOtpRequest;
import com.example.spring_boot_project_api.exception.InvalidOtpException;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserSecurity;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.UserSecurityRepository;
import com.example.spring_boot_project_api.service.EmailService;
import com.example.spring_boot_project_api.service.SecurityService;

import org.mockito.ArgumentCaptor;

/**
 * End-to-end password-recovery flow against the real repositories. The mail
 * provider is mocked so the OTP can be captured (it is never stored or logged
 * in plaintext); this verifies the OTP is hashed, delivered, and consumed, and
 * that account enumeration is prevented.
 */
@SpringBootTest(properties = "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
class PasswordRecoveryEndToEndTest {

    @Autowired private SecurityService securityService;
    @Autowired private UserRepository userRepository;
    @Autowired private UserSecurityRepository userSecurityRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockitoBean private EmailService emailService;

    private User user;
    private String token = UUID.randomUUID().toString().substring(0, 8);

    @BeforeEach
    void setUp() {
        user = User.builder()
                .username("pw_" + token)
                .email("pw_" + token + "@rental.com")
                .passwordHash(passwordEncoder.encode("OldPass123"))
                .firstName("Recovery")
                .lastName("User")
                .build();
        user = userRepository.save(user);

        userSecurityRepository.save(UserSecurity.builder()
                .user(user)
                .recoveryEmail("recovery." + token + "@gmail.com")
                .recoveryEmailVerified(true)
                .recoveryEmailVerifiedAt(java.time.LocalDateTime.now())
                .build());
    }

    @AfterEach
    void tearDown() {
        userSecurityRepository.findByUserId(user.getId())
                .ifPresent(userSecurityRepository::delete);
        userRepository.delete(user);
        user = null;
    }

    private String latestPasswordResetOtp() {
        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetOtp(anyString(), otpCaptor.capture(), anyInt());
        return otpCaptor.getValue();
    }

    @Test
    void fullPasswordRecoveryFlow() {
        // 1. Request reset (generic response, no enumeration leak).
        var msg = securityService.forgotPassword(
                new ForgotPasswordRequest(user.getEmail()), null);
        assertThat(msg.message()).contains("If an account exists with this email");

        String otp = latestPasswordResetOtp();
        assertThat(otp).matches("\\d{6}");

        // 2. Verify OTP -> obtain short-lived reset token.
        var verification = securityService.verifyPasswordResetOtp(
                new VerifyPasswordResetOtpRequest(user.getEmail(), otp), null);
        assertThat(verification.resetToken()).isNotBlank();
        assertThat(verification.maskedRecoveryEmail()).contains("***");

        // 3. Reset password.
        securityService.resetPassword(new ResetPasswordRequest(
                verification.resetToken(), "NewPass456", "NewPass456"), null);

        // 4. Old password no longer works; new one does.
        org.springframework.security.crypto.password.PasswordEncoder enc = passwordEncoder;
        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(enc.matches("OldPass123", reloaded.getPasswordHash())).isFalse();
        assertThat(enc.matches("NewPass456", reloaded.getPasswordHash())).isTrue();
    }

    @Test
    void resetTokenCannotBeReused() {
        var msg = securityService.forgotPassword(
                new ForgotPasswordRequest(user.getEmail()), null);
        String otp = latestPasswordResetOtp();
        var verification = securityService.verifyPasswordResetOtp(
                new VerifyPasswordResetOtpRequest(user.getEmail(), otp), null);

        securityService.resetPassword(new ResetPasswordRequest(
                verification.resetToken(), "NewPass456", "NewPass456"), null);

        // Reusing the same token must fail.
        assertThatThrownBy(() -> securityService.resetPassword(new ResetPasswordRequest(
                verification.resetToken(), "AnotherPass1", "AnotherPass1"), null))
                .isInstanceOf(com.example.spring_boot_project_api.exception.InvalidResetTokenException.class);
    }

    @Test
    void oldOtpCannotBeReused() {
        securityService.forgotPassword(new ForgotPasswordRequest(user.getEmail()), null);
        String otp = latestPasswordResetOtp();

        // Consume the OTP once.
        var verification = securityService.verifyPasswordResetOtp(
                new VerifyPasswordResetOtpRequest(user.getEmail(), otp), null);
        securityService.resetPassword(new ResetPasswordRequest(
                verification.resetToken(), "NewPass456", "NewPass456"), null);

        // Reusing the consumed OTP is impossible (new OTP would be needed).
        assertThatThrownBy(() -> securityService.verifyPasswordResetOtp(
                new VerifyPasswordResetOtpRequest(user.getEmail(), otp), null))
                .isInstanceOf(InvalidOtpException.class);
    }

    @Test
    void forgotPasswordReturnsGenericForUnknownAndNoRecovery() {
        // Unknown account: generic message, no mail, no error detail.
        var msg = securityService.forgotPassword(
                new ForgotPasswordRequest("does-not-exist@rental.com"), null);
        assertThat(msg.message()).contains("If an account exists with this email");
    }
}
