package com.example.spring_boot_project_api.service.impl;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.config.SecurityProperties;
import com.example.spring_boot_project_api.dto.request.AddRecoveryEmailRequest;
import com.example.spring_boot_project_api.dto.request.ForgotPasswordRequest;
import com.example.spring_boot_project_api.dto.request.ResetPasswordRequest;
import com.example.spring_boot_project_api.dto.request.VerifyPasswordResetOtpRequest;
import com.example.spring_boot_project_api.dto.request.VerifyRecoveryEmailRequest;
import com.example.spring_boot_project_api.dto.response.MessageResponse;
import com.example.spring_boot_project_api.dto.response.ResetOtpVerificationResponse;
import com.example.spring_boot_project_api.dto.response.UserSecurityResponse;
import com.example.spring_boot_project_api.enums.OtpPurpose;
import com.example.spring_boot_project_api.enums.TwoFactorMethod;
import com.example.spring_boot_project_api.exception.EmailDeliveryException;
import com.example.spring_boot_project_api.exception.InvalidPasswordException;
import com.example.spring_boot_project_api.exception.InvalidResetTokenException;
import com.example.spring_boot_project_api.exception.RateLimitExceededException;
import com.example.spring_boot_project_api.exception.RecoveryEmailException;
import com.example.spring_boot_project_api.exception.TwoFactorException;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserSecurity;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.UserSecurityRepository;
import com.example.spring_boot_project_api.service.EmailService;
import com.example.spring_boot_project_api.service.SecurityService;
import com.example.spring_boot_project_api.util.EmailValidator;
import com.example.spring_boot_project_api.util.SecurityCodeUtil;
import com.example.spring_boot_project_api.util.SecurityUtils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SecurityServiceImpl implements SecurityService {

    private static final String GENERIC_FORGOT_MESSAGE =
            "If an account exists with this email, recovery instructions have been sent.";

    private final UserRepository userRepository;
    private final UserSecurityRepository userSecurityRepository;
    private final EmailService emailService;
    private final OtpService otpService;
    private final OneTimeTokenStore tokenStore;
    private final RateLimiterService rateLimiter;
    private final SecurityProperties properties;
    private final SecurityUtils securityUtils;
    private final PasswordEncoder passwordEncoder;
    private final SecurityAuditLogger audit;

    // ----- Recovery email -----

    @Override
    @Transactional(readOnly = true)
    public UserSecurityResponse getMySecurity() {
        User user = securityUtils.getCurrentUser();
        return userSecurityRepository.findByUserId(user.getId())
                .map(this::toResponse)
                .orElseGet(() -> new UserSecurityResponse(null, false, null, false, null));
    }

    @Override
    @Transactional
    public MessageResponse addRecoveryEmail(AddRecoveryEmailRequest request, HttpServletRequest http) {
        User user = securityUtils.getCurrentUser();
        UserSecurity security = getOrCreateSecurity(user);

        String newEmail = request.recoveryEmail().trim().toLowerCase();
        if (newEmail.equalsIgnoreCase(user.getEmail())) {
            throw new RecoveryEmailException(
                    "Recovery email cannot be the same as the system/login email.");
        }

        // Recovery email must be a real, externally reachable inbox. It must never
        // be on the corporate login-only domain (@rental.com), which has no real
        // mailbox. Any other domain (Gmail, Outlook, Yahoo, ...) is allowed.
        if (EmailValidator.isOnInternalDomain(newEmail)) {
            throw new RecoveryEmailException(
                    "Recovery email must be a real external email (e.g. Gmail), "
                            + "not a @" + EmailValidator.INTERNAL_DOMAIN + " address.");
        }

        // A verified recovery email is never silently replaced: any new address is
        // only committed once it is itself OTP-verified (see verifyRecoveryEmail).
        if (Boolean.TRUE.equals(security.getRecoveryEmailVerified())
                && newEmail.equalsIgnoreCase(security.getRecoveryEmail())) {
            throw new RecoveryEmailException("This email is already your verified recovery email.");
        }

        enforceResendCooldown(user.getId(), OtpPurpose.EMAIL_VERIFICATION, http);

        String otp = otpService.issueOtp(user, OtpPurpose.EMAIL_VERIFICATION);
        security.setPendingRecoveryEmail(newEmail);
        security.setRecoveryEmailVerified(false);
        security.setRecoveryEmailVerifiedAt(null);
        userSecurityRepository.save(security);

        emailService.sendRecoveryEmailVerificationOtp(newEmail, otp,
                minutes(properties.getOtpTtl().toSeconds()));
        audit.log(user.getId(), "RECOVERY_EMAIL_ADD_REQUESTED", http,
                "pending=" + maskEmail(newEmail));

        return MessageResponse.of(
                "A verification code has been sent to the provided email address.");
    }

    @Override
    @Transactional
    public MessageResponse verifyRecoveryEmail(VerifyRecoveryEmailRequest request,
            HttpServletRequest http) {
        User user = securityUtils.getCurrentUser();
        UserSecurity security = getOrCreateSecurity(user);

        String pending = security.getPendingRecoveryEmail();
        if (pending == null
                || !pending.equalsIgnoreCase(request.recoveryEmail().trim().toLowerCase())) {
            throw new RecoveryEmailException(
                    "No pending verification for this email. Request a new code.");
        }

        enforceVerifyRateLimit(user.getId(), "verify-recovery-email:" + pending, http);

        try {
            otpService.verifyOtp(user, OtpPurpose.EMAIL_VERIFICATION, request.otp());
        } catch (RuntimeException ex) {
            audit.log(user.getId(), "RECOVERY_EMAIL_VERIFY_FAILED", http,
                    "pending=" + maskEmail(pending));
            throw ex;
        }

        security.setRecoveryEmail(pending);
        security.setRecoveryEmailVerified(true);
        security.setRecoveryEmailVerifiedAt(LocalDateTime.now());
        security.setPendingRecoveryEmail(null);
        userSecurityRepository.save(security);

        audit.log(user.getId(), "RECOVERY_EMAIL_VERIFIED", http, "email=" + maskEmail(pending));
        return MessageResponse.of("Your recovery email has been verified.");
    }

    // ----- Forgot password -----

    @Override
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request, HttpServletRequest http) {
        String email = request.email().trim().toLowerCase();

        // The response is always the same, whether or not the account/recovery
        // email exists, to prevent account enumeration.
        userRepository.findByEmail(email).ifPresent(user -> {
            try {
                if (rateLimiter.tryAcquire("forgot-password:" + user.getId(),
                        properties.getMaxRequestsPerIp(), properties.getRateLimitWindow())) {
                    sendResetOtpIfEligible(user, http);
                }
            } catch (RateLimitExceededException | EmailDeliveryException ignored) {
                // Keep the outward response identical and enumeration-safe.
            }
        });

        return MessageResponse.of(GENERIC_FORGOT_MESSAGE);
    }

    @Override
    @Transactional
    public ResetOtpVerificationResponse verifyPasswordResetOtp(VerifyPasswordResetOtpRequest request,
            HttpServletRequest http) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidResetTokenException("Invalid or expired verification code."));

        if (otpService.isWithinResendCooldown(user.getId(), OtpPurpose.PASSWORD_RESET)) {
            // no-op; still allow verifying the existing code
        }

        try {
            otpService.verifyOtp(user, OtpPurpose.PASSWORD_RESET, request.otp());
        } catch (RuntimeException ex) {
            audit.log(user.getId(), "PASSWORD_RESET_OTP_FAILED", http, "email=" + maskEmail(email));
            throw ex;
        }

        UserSecurity security = getOrCreateSecurity(user);
        String resetToken = SecurityCodeUtil.generateToken();
        tokenStore.put(resetToken, "PASSWORD_RESET", user.getId(),
                java.time.Instant.now().plus(properties.getResetTokenTtl()));

        audit.log(user.getId(), "PASSWORD_RESET_OTP_VERIFIED", http,
                "email=" + maskEmail(email));
        return new ResetOtpVerificationResponse(
                resetToken,
                properties.getResetTokenTtl().toSeconds(),
                maskEmail(deliveryEmail(user, security)));
    }

    @Override
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request, HttpServletRequest http) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new InvalidPasswordException("Password confirmation does not match.");
        }

        Long userId = tokenStore.consume(request.resetToken(), "PASSWORD_RESET");
        if (userId == null) {
            audit.log(null, "PASSWORD_RESET_TOKEN_FAILED", http, "reason=invalid_or_used");
            throw new InvalidResetTokenException(
                    "The reset link is invalid or has expired. Please start over.");
        }

        User user = findUser(userId);
        validatePasswordPolicy(user, request.newPassword());

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Invalidate outstanding OTPs, reset tokens and any old sessions/tokens.
        otpService.invalidateOtpFor(user.getId(), OtpPurpose.PASSWORD_RESET);
        tokenStore.invalidate("PASSWORD_RESET", user.getId());

        audit.log(user.getId(), "PASSWORD_RESET_COMPLETED", http, "email=" + maskEmail(user.getEmail()));
        return MessageResponse.of("Your password has been reset successfully.");
    }

    // ----- Two-factor auth -----

    @Override
    @Transactional
    public MessageResponse enableTwoFactor(HttpServletRequest http) {
        User user = securityUtils.getCurrentUser();
        UserSecurity security = getOrCreateSecurity(user);

        // Internal users (non-TENANT) must verify a real recovery inbox first,
        // because their login email has no real mailbox. Tenant accounts use
        // their primary login email and need no separate recovery email.
        boolean isTenant = securityUtils.hasRole(user.getId(), EmailValidator.TENANT_ROLE);
        if (!isTenant && !Boolean.TRUE.equals(security.getRecoveryEmailVerified())) {
            throw new TwoFactorException(
                    "You must verify a recovery email before enabling two-factor authentication.");
        }

        security.setTwoFactorEnabled(true);
        security.setTwoFactorMethod(TwoFactorMethod.EMAIL);
        userSecurityRepository.save(security);

        audit.log(user.getId(), "TWO_FACTOR_ENABLED", http, "method=EMAIL");
        return MessageResponse.of("Two-factor authentication enabled.");
    }

    @Override
    @Transactional
    public MessageResponse disableTwoFactor(HttpServletRequest http) {
        User user = securityUtils.getCurrentUser();
        UserSecurity security = getOrCreateSecurity(user);

        if (userHasRequiredRole(user.getId())) {
            throw new TwoFactorException(
                    "Two-factor authentication is required for your role and cannot be disabled.");
        }

        security.setTwoFactorEnabled(false);
        security.setTwoFactorMethod(null);
        userSecurityRepository.save(security);

        audit.log(user.getId(), "TWO_FACTOR_DISABLED", http, null);
        return MessageResponse.of("Two-factor authentication disabled.");
    }

    // ----- Login 2FA integration (called by AuthService) -----

    /** Whether 2FA is required (and possible) for the given user on login. */
    public boolean requiresTwoFactor(User user) {
        UserSecurity security = userSecurityRepository.findByUserId(user.getId()).orElse(null);
        if (security == null || !Boolean.TRUE.equals(security.getTwoFactorEnabled())) {
            return false;
        }
        // One-time codes are delivered to the verified recovery email for internal
        // accounts, or to the primary login email for tenant accounts, so no
        // recovery-email precondition applies here.
        return true;
    }

    public TwoFactorMethod twoFactorMethodOf(User user) {
        return TwoFactorMethod.EMAIL;
    }

    /**
     * Issues OTP toward 2FA for the authenticated challenge and returns the
     * short-lived challenge token to be returned to the client.
     */
    public com.example.spring_boot_project_api.dto.response.TwoFactorChallengeResponse
            createTwoFactorChallenge(User user, HttpServletRequest http) {
        UserSecurity security = userSecurityRepository.findByUserId(user.getId())
                .orElseThrow(() -> new TwoFactorException("Two-factor authentication is not configured."));

        enforceResendCooldown(user.getId(), OtpPurpose.TWO_FACTOR_LOGIN, http);
        String otp = otpService.issueOtp(user, OtpPurpose.TWO_FACTOR_LOGIN);
        emailService.sendTwoFactorOtp(deliveryEmail(user, security), otp,
                minutes(properties.getOtpTtl().toSeconds()));

        String challengeToken = SecurityCodeUtil.generateToken();
        tokenStore.put(challengeToken, "TWO_FACTOR_CHALLENGE", user.getId(),
                java.time.Instant.now().plus(properties.getChallengeTokenTtl()));

        audit.log(user.getId(), "TWO_FACTOR_CHALLENGE_ISSUED", http, "method=EMAIL");
        return new com.example.spring_boot_project_api.dto.response.TwoFactorChallengeResponse(
                true,
                challengeToken,
                properties.getChallengeTokenTtl().toSeconds(),
                TwoFactorMethod.EMAIL.name());
    }

    /**
     * Verifies a 2FA OTP against a challenge token. On success returns the user
     * id for which authentication is now authorized (the challenge is consumed).
     */
    public Long verifyTwoFactor(String challengeToken, String otp, HttpServletRequest http) {
        Long userId = tokenStore.consume(challengeToken, "TWO_FACTOR_CHALLENGE");
        if (userId == null) {
            audit.log(null, "TWO_FACTOR_VERIFY_FAILED", http, "reason=invalid_challenge");
            throw new com.example.spring_boot_project_api.exception.TwoFactorChallengeException(
                    "The sign-in session has expired. Please sign in again.");
        }

        User user = findUser(userId);
        try {
            otpService.verifyOtp(user, OtpPurpose.TWO_FACTOR_LOGIN, otp);
        } catch (RuntimeException ex) {
            audit.log(user.getId(), "TWO_FACTOR_VERIFY_FAILED", http, "reason=otp");
            throw ex;
        }

        audit.log(user.getId(), "TWO_FACTOR_VERIFY_SUCCEEDED", http, null);
        return user.getId();
    }

    // ----- Helpers -----

    private UserSecurity getOrCreateSecurity(User user) {
        return userSecurityRepository.findByUserId(user.getId())
                .orElseGet(() -> userSecurityRepository.save(
                        UserSecurity.builder().user(user).build()));
    }

    private void enforceResendCooldown(Long userId, OtpPurpose purpose, HttpServletRequest http) {
        if (otpService.isWithinResendCooldown(userId, purpose)) {
            audit.log(userId, "OTP_RESEND_BLOCKED", http, "purpose=" + purpose);
            throw new RateLimitExceededException(
                    "Please wait before requesting another code.");
        }
    }

    private void sendResetOtpIfEligible(User user, HttpServletRequest http) {
        UserSecurity security = userSecurityRepository.findByUserId(user.getId()).orElse(null);
        String deliverTo = deliveryEmail(user, security);
        if (deliverTo == null || deliverTo.isBlank()) {
            audit.log(user.getId(), "PASSWORD_RESET_SKIPPED_NO_RECIPIENT", http, null);
            return;
        }

        enforceResendCooldown(user.getId(), OtpPurpose.PASSWORD_RESET, http);
        String otp = otpService.issueOtp(user, OtpPurpose.PASSWORD_RESET);
        emailService.sendPasswordResetOtp(deliverTo, otp,
                minutes(properties.getOtpTtl().toSeconds()));
        audit.log(user.getId(), "PASSWORD_RESET_REQUESTED", http,
                "to=" + maskEmail(deliverTo));
    }

    /**
     * The inbox to deliver password-reset and 2FA codes to. Internal users
     * receive codes on their (verified) recovery email; tenant accounts have no
     * separate recovery email, so codes go to their primary login email.
     */
    private String deliveryEmail(User user, UserSecurity security) {
        if (security != null && Boolean.TRUE.equals(security.getRecoveryEmailVerified())
                && security.getRecoveryEmail() != null) {
            return security.getRecoveryEmail();
        }
        return user.getEmail();
    }

    private void enforceVerifyRateLimit(Long userId, String key, HttpServletRequest http) {
        if (!rateLimiter.tryAcquire(key, properties.getMaxRequestsPerIp(),
                properties.getRateLimitWindow())) {
            audit.log(userId, "VERIFY_RATE_LIMITED", http, "key=" + key);
            throw new RateLimitExceededException("Too many attempts. Please try again later.");
        }
    }

    private void validatePasswordPolicy(User user, String newPassword) {
        if (newPassword.length() < 8) {
            throw new InvalidPasswordException("Password must be at least 8 characters.");
        }
        if (newPassword.equalsIgnoreCase(user.getEmail())
                || newPassword.equalsIgnoreCase(user.getUsername())) {
            throw new InvalidPasswordException("Password cannot be the same as your email or username.");
        }
    }

    private boolean userHasRequiredRole(Long userId) {
        for (String role : properties.getTwoFactorRequiredRoles()) {
            if (securityUtils.hasRole(userId, role)) {
                return true;
            }
        }
        return false;
    }

    private static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return email;
        }
        String local = email.substring(0, at);
        String domain = email.substring(at);
        return local.charAt(0) + "***" + domain;
    }

    private static int minutes(long seconds) {
        return (int) Math.max(1, Math.round(seconds / 60.0));
    }

    private User findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> UserNotFoundException.forId(id));
    }

    private UserSecurityResponse toResponse(UserSecurity security) {
        return new UserSecurityResponse(
                security.getRecoveryEmail(),
                Boolean.TRUE.equals(security.getRecoveryEmailVerified()),
                security.getRecoveryEmailVerifiedAt(),
                Boolean.TRUE.equals(security.getTwoFactorEnabled()),
                security.getTwoFactorMethod() == null ? null : security.getTwoFactorMethod().name());
    }
}
