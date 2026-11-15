package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.spring_boot_project_api.config.SecurityProperties;
import com.example.spring_boot_project_api.enums.OtpPurpose;
import com.example.spring_boot_project_api.exception.InvalidOtpException;
import com.example.spring_boot_project_api.exception.RateLimitExceededException;
import com.example.spring_boot_project_api.model.OtpVerification;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.OtpVerificationRepository;
import com.example.spring_boot_project_api.service.impl.OtpService;
import com.example.spring_boot_project_api.service.impl.RateLimiterService;
import com.example.spring_boot_project_api.util.SecurityCodeUtil;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private OtpVerificationRepository otpRepository;

    @Mock
    private RateLimiterService rateLimiter;

    private SecurityProperties properties;

    private OtpService otpService;

    private User user;

    @BeforeEach
    void setUp() {
        properties = new SecurityProperties();
        otpService = new OtpService(otpRepository, properties, rateLimiter);
        user = User.builder().id(7L).build();
    }

    private OtpVerification activeOtp(String hash, LocalDateTime expiresAt, int attempts) {
        return OtpVerification.builder()
                .id(1L)
                .user(user)
                .purpose(OtpPurpose.PASSWORD_RESET)
                .otpHash(hash)
                .expiresAt(expiresAt)
                .attempts(attempts)
                .maxAttempts(properties.getOtpMaxAttempts())
                .build();
    }

    @Test
    void issueOtpShouldReturnSixDigitsAndHashStored() {
        when(otpRepository.countByUserIdAndPurposeAndCreatedAtAfter(any(), any(), any()))
                .thenReturn(0L);

        String otp = otpService.issueOtp(user, OtpPurpose.PASSWORD_RESET);

        assertThat(otp).matches("\\d{6}");
        ArgumentCaptor<OtpVerification> captor = ArgumentCaptor.forClass(OtpVerification.class);
        verify(otpRepository).save(captor.capture());
        // Stored value is a hash, never the plaintext OTP.
        assertThat(captor.getValue().getOtpHash())
                .isEqualTo(SecurityCodeUtil.hashSecret(otp))
                .isNotEqualTo(otp);
    }

    @Test
    void issueOtpShouldThrowWhenSendWindowExhausted() {
        when(otpRepository.countByUserIdAndPurposeAndCreatedAtAfter(any(), any(), any()))
                .thenReturn((long) properties.getOtpMaxSendsPerWindow());

        assertThatThrownBy(() -> otpService.issueOtp(user, OtpPurpose.PASSWORD_RESET))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void verifyOtpShouldSucceedAndConsume() {
        String otp = "123456";
        when(otpRepository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(7L, OtpPurpose.PASSWORD_RESET))
                .thenReturn(java.util.Optional.of(activeOtp(
                        SecurityCodeUtil.hashSecret(otp), LocalDateTime.now().plusMinutes(10), 0)));

        otpService.verifyOtp(user, OtpPurpose.PASSWORD_RESET, otp);

        ArgumentCaptor<OtpVerification> captor = ArgumentCaptor.forClass(OtpVerification.class);
        verify(otpRepository).save(captor.capture());
        assertThat(captor.getValue().getConsumedAt()).isNotNull();
    }

    @Test
    void verifyOtpShouldRejectExpired() {
        String otp = "123456";
        when(otpRepository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(7L, OtpPurpose.PASSWORD_RESET))
                .thenReturn(java.util.Optional.of(activeOtp(
                        SecurityCodeUtil.hashSecret(otp), LocalDateTime.now().minusMinutes(1), 0)));

        assertThatThrownBy(() -> otpService.verifyOtp(user, OtpPurpose.PASSWORD_RESET, otp))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void verifyOtpShouldRejectConsumed() {
        String otp = "123456";
        OtpVerification consumed = activeOtp(SecurityCodeUtil.hashSecret(otp),
                LocalDateTime.now().plusMinutes(10), 0);
        consumed.setConsumedAt(LocalDateTime.now());
        when(otpRepository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(7L, OtpPurpose.PASSWORD_RESET))
                .thenReturn(java.util.Optional.of(consumed));

        assertThatThrownBy(() -> otpService.verifyOtp(user, OtpPurpose.PASSWORD_RESET, otp))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessageContaining("already been used");
    }

    @Test
    void verifyOtpShouldRejectWrongCodeAndCountAttempt() {
        when(otpRepository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(7L, OtpPurpose.PASSWORD_RESET))
                .thenReturn(java.util.Optional.of(activeOtp(
                        SecurityCodeUtil.hashSecret("999999"), LocalDateTime.now().plusMinutes(10), 0)));

        assertThatThrownBy(() -> otpService.verifyOtp(user, OtpPurpose.PASSWORD_RESET, "123456"))
                .isInstanceOf(InvalidOtpException.class);

        ArgumentCaptor<OtpVerification> captor = ArgumentCaptor.forClass(OtpVerification.class);
        verify(otpRepository).save(captor.capture());
        assertThat(captor.getValue().getAttempts()).isEqualTo(1);
    }

    @Test
    void verifyOtpShouldInvalidateAfterMaxAttempts() {
        when(otpRepository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(7L, OtpPurpose.PASSWORD_RESET))
                .thenReturn(java.util.Optional.of(activeOtp(
                        SecurityCodeUtil.hashSecret("999999"), LocalDateTime.now().plusMinutes(10),
                        properties.getOtpMaxAttempts() - 1)));

        assertThatThrownBy(() -> otpService.verifyOtp(user, OtpPurpose.PASSWORD_RESET, "123456"))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessageContaining("Too many incorrect attempts");

        ArgumentCaptor<OtpVerification> captor = ArgumentCaptor.forClass(OtpVerification.class);
        verify(otpRepository).save(captor.capture());
        // Consumed to invalidate: no further guesses possible.
        assertThat(captor.getValue().getConsumedAt()).isNotNull();
    }

    @Test
    void verifyOtpShouldRejectOtpForOtherPurpose() {
        // PASSWORD_RESET OTP record, but verify with TWO_FACTOR_LOGIN purpose.
        when(otpRepository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(7L, OtpPurpose.TWO_FACTOR_LOGIN))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> otpService.verifyOtp(user, OtpPurpose.TWO_FACTOR_LOGIN, "123456"))
                .isInstanceOf(InvalidOtpException.class);
    }

    @Test
    void invalidateOtpForShouldRemoveOutstanding() {
        otpService.invalidateOtpFor(7L, OtpPurpose.PASSWORD_RESET);
        verify(otpRepository).deleteByUserIdAndPurpose(7L, OtpPurpose.PASSWORD_RESET);
    }

    @Test
    void resendCooldownShouldBeDetected() {
        OtpVerification recent = activeOtp("x", LocalDateTime.now().plusMinutes(10), 0);
        recent.setCreatedAt(LocalDateTime.now());
        when(otpRepository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(7L, OtpPurpose.PASSWORD_RESET))
                .thenReturn(java.util.Optional.of(recent));

        assertThat(otpService.isWithinResendCooldown(7L, OtpPurpose.PASSWORD_RESET)).isTrue();
    }

    @Test
    void resendCooldownShouldExpire() {
        OtpVerification old = activeOtp("x", LocalDateTime.now().plusMinutes(10), 0);
        old.setCreatedAt(LocalDateTime.now().minusSeconds(120));
        when(otpRepository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(7L, OtpPurpose.PASSWORD_RESET))
                .thenReturn(java.util.Optional.of(old));

        assertThat(otpService.isWithinResendCooldown(7L, OtpPurpose.PASSWORD_RESET)).isFalse();
    }
}
