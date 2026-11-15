package com.example.spring_boot_project_api.service.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.config.SecurityProperties;
import com.example.spring_boot_project_api.enums.OtpPurpose;
import com.example.spring_boot_project_api.exception.InvalidOtpException;
import com.example.spring_boot_project_api.exception.RateLimitExceededException;
import com.example.spring_boot_project_api.model.OtpVerification;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.OtpVerificationRepository;
import com.example.spring_boot_project_api.util.SecurityCodeUtil;

import lombok.RequiredArgsConstructor;

/**
 * Handles OTP generation, hashing, persistence, verification, expiration,
 * attempt limits, consume-on-use, and send throttling. OTPs are never logged or
 * returned after creation beyond the single delivery call.
 */
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpVerificationRepository otpRepository;
    private final SecurityProperties properties;
    private final RateLimiterService rateLimiter;

    /**
     * Generates and stores a new OTP for the given user/purpose, applying the
     * resend cooldown and per-account send-window limits. The raw OTP is
     * returned exactly once for delivery.
     */
    @Transactional
    public String issueOtp(User user, OtpPurpose purpose) {
        long sentWithinWindow = otpRepository.countByUserIdAndPurposeAndCreatedAtAfter(
                user.getId(), purpose, LocalDateTime.now().minus(properties.getOtpSendWindow()));
        if (sentWithinWindow >= properties.getOtpMaxSendsPerWindow()) {
            throw new RateLimitExceededException("Too many verification codes requested. Try again later.");
        }

        String otp = SecurityCodeUtil.generateOtp();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plus(properties.getOtpTtl());
        int maxAttempts = properties.getOtpMaxAttempts();

        otpRepository.save(OtpVerification.builder()
                .user(user)
                .purpose(purpose)
                .otpHash(SecurityCodeUtil.hashSecret(otp))
                .expiresAt(expiresAt)
                .maxAttempts(maxAttempts)
                .attempts(0)
                .build());
        return otp;
    }

    /**
     * Checks the resend cooldown for a user/purpose (whether an active OTP was
     * issued too recently to allow another send).
     */
    public boolean isWithinResendCooldown(Long userId, OtpPurpose purpose) {
        return otpRepository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(userId, purpose)
                .map(o -> o.getCreatedAt().plus(properties.getOtpResendCooldown())
                        .isAfter(LocalDateTime.now()))
                .orElse(false);
    }

    /**
     * Verifies the given raw OTP for the user/purpose. Validates the latest
     * active OTP, expiration, unused state, and attempt count. Consumes the OTP
     * on success or increments attempts on failure (invalidating when max is
     * reached). Throws {@link InvalidOtpException} on any failure.
     */
    @Transactional
    public void verifyOtp(User user, OtpPurpose purpose, String rawOtp) {
        OtpVerification otp = otpRepository
                .findFirstByUserIdAndPurposeOrderByCreatedAtDesc(user.getId(), purpose)
                .orElseThrow(() -> new InvalidOtpException("Invalid or expired verification code."));

        if (otp.getConsumedAt() != null) {
            throw new InvalidOtpException("This verification code has already been used.");
        }
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidOtpException("This verification code has expired. Request a new one.");
        }

        boolean match = SecurityCodeUtil.secureEquals(
                otp.getOtpHash(), SecurityCodeUtil.hashSecret(rawOtp));

        if (!match) {
            int attempts = otp.getAttempts() + 1;
            if (attempts >= otp.getMaxAttempts()) {
                // Invalidate by consuming it so no further guesses are possible.
                otp.setConsumedAt(LocalDateTime.now());
                otpRepository.save(otp);
                throw new InvalidOtpException("Too many incorrect attempts. Request a new code.");
            }
            otp.setAttempts(attempts);
            otpRepository.save(otp);
            throw new InvalidOtpException("Invalid or expired verification code.");
        }

        otp.setConsumedAt(LocalDateTime.now());
        otpRepository.save(otp);
    }

    /** Invalidates all outstanding OTPs for a user/purpose (e.g. after reset). */
    @Transactional
    public void invalidateOtpFor(Long userId, OtpPurpose purpose) {
        otpRepository.deleteByUserIdAndPurpose(userId, purpose);
    }
}
