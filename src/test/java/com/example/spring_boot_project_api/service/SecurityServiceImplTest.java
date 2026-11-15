package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.spring_boot_project_api.config.SecurityProperties;
import com.example.spring_boot_project_api.dto.request.AddRecoveryEmailRequest;
import com.example.spring_boot_project_api.dto.request.ForgotPasswordRequest;
import com.example.spring_boot_project_api.dto.request.ResetPasswordRequest;
import com.example.spring_boot_project_api.dto.request.VerifyPasswordResetOtpRequest;
import com.example.spring_boot_project_api.dto.response.MessageResponse;
import com.example.spring_boot_project_api.enums.OtpPurpose;
import com.example.spring_boot_project_api.enums.TwoFactorMethod;
import com.example.spring_boot_project_api.exception.InvalidOtpException;
import com.example.spring_boot_project_api.exception.InvalidResetTokenException;
import com.example.spring_boot_project_api.exception.RecoveryEmailException;
import com.example.spring_boot_project_api.exception.TwoFactorException;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserSecurity;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.UserSecurityRepository;
import com.example.spring_boot_project_api.service.impl.OtpService;
import com.example.spring_boot_project_api.service.impl.OneTimeTokenStore;
import com.example.spring_boot_project_api.service.impl.RateLimiterService;
import com.example.spring_boot_project_api.service.impl.SecurityAuditLogger;
import com.example.spring_boot_project_api.service.impl.SecurityServiceImpl;
import com.example.spring_boot_project_api.util.SecurityCodeUtil;
import com.example.spring_boot_project_api.util.SecurityUtils;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class SecurityServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserSecurityRepository userSecurityRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private OtpService otpService;
    @Mock
    private OneTimeTokenStore tokenStore;
    @Mock
    private RateLimiterService rateLimiter;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SecurityAuditLogger audit;
    @Mock
    private HttpServletRequest http;

    private SecurityProperties properties;
    private SecurityServiceImpl service;
    private User user;

    @BeforeEach
    void setUp() {
        properties = new SecurityProperties();
        service = new SecurityServiceImpl(
                userRepository, userSecurityRepository, emailService, otpService,
                tokenStore, rateLimiter, properties, securityUtils, passwordEncoder, audit);
        user = User.builder().id(5L).username("john").email("john@rental.com").build();
    }

    private UserSecurity verifiedSecurity() {
        return UserSecurity.builder()
                .id(1L)
                .user(user)
                .recoveryEmail("john.doe@gmail.com")
                .recoveryEmailVerified(true)
                .recoveryEmailVerifiedAt(LocalDateTime.now())
                .twoFactorEnabled(false)
                .twoFactorMethod(null)
                .build();
    }

    @Test
    void forgotPasswordShouldReturnGenericResponseForUnknownEmail() {
        when(userRepository.findByEmail("ghost@rental.com")).thenReturn(Optional.empty());

        MessageResponse response = service.forgotPassword(
                new ForgotPasswordRequest("ghost@rental.com"), http);

        assertThat(response.message()).contains("If an account exists with this email");
        verify(userRepository, never()).findById(any());
    }

    @Test
    void forgotPasswordShouldSendOtpToPrimaryEmailWhenAccountHasNoRecovery() {
        // A tenant (or internal account without a recovery inbox) receives the
        // reset OTP on its primary login email instead.
        User plain = User.builder().id(2L).email("john@gmail.com").build();
        when(userRepository.findByEmail("john@gmail.com")).thenReturn(Optional.of(plain));
        when(userSecurityRepository.findByUserId(2L)).thenReturn(Optional.empty());
        when(rateLimiter.tryAcquire(any(), any(Integer.class), any())).thenReturn(true);
        when(otpService.isWithinResendCooldown(2L, OtpPurpose.PASSWORD_RESET)).thenReturn(false);
        when(otpService.issueOtp(plain, OtpPurpose.PASSWORD_RESET)).thenReturn("444555");

        MessageResponse response = service.forgotPassword(
                new ForgotPasswordRequest("john@gmail.com"), http);

        assertThat(response.message()).contains("If an account exists with this email");
        verify(emailService).sendPasswordResetOtp(eq("john@gmail.com"), eq("444555"), anyInt());
    }

    @Test
    void forgotPasswordShouldSendOtpToVerifiedRecovery() {
        UserSecurity security = verifiedSecurity();
        when(userRepository.findByEmail("john@rental.com")).thenReturn(Optional.of(user));
        when(userSecurityRepository.findByUserId(5L)).thenReturn(Optional.of(security));
        when(rateLimiter.tryAcquire(any(), any(Integer.class), any())).thenReturn(true);
        when(otpService.isWithinResendCooldown(5L, OtpPurpose.PASSWORD_RESET)).thenReturn(false);
        when(otpService.issueOtp(user, OtpPurpose.PASSWORD_RESET)).thenReturn("111222");

        service.forgotPassword(new ForgotPasswordRequest("john@rental.com"), http);

        verify(emailService).sendPasswordResetOtp(eq("john.doe@gmail.com"), eq("111222"), anyInt());
    }

    @Test
    void verifyPasswordResetOtpShouldReturnResetToken() {
        UserSecurity security = verifiedSecurity();
        when(userRepository.findByEmail("john@rental.com")).thenReturn(Optional.of(user));
        when(userSecurityRepository.findByUserId(5L)).thenReturn(Optional.of(security));
        when(otpService.isWithinResendCooldown(5L, OtpPurpose.PASSWORD_RESET)).thenReturn(false);

        var response = service.verifyPasswordResetOtp(
                new VerifyPasswordResetOtpRequest("john@rental.com", "123456"), http);

        assertThat(response.resetToken()).isNotBlank();
        assertThat(response.maskedRecoveryEmail()).contains("***");
    }

    @Test
    void verifyPasswordResetOtpShouldFailForUnknownEmail() {
        when(userRepository.findByEmail("ghost@rental.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyPasswordResetOtp(
                new VerifyPasswordResetOtpRequest("ghost@rental.com", "123456"), http))
                .isInstanceOf(InvalidResetTokenException.class);
    }

    @Test
    void resetPasswordShouldRejectMismatchedConfirmation() {
        assertThatThrownBy(() -> service.resetPassword(
                new ResetPasswordRequest("tok", "NewPassword1", "Different1"), http))
                .isInstanceOf(com.example.spring_boot_project_api.exception.InvalidPasswordException.class);
    }

    @Test
    void resetPasswordShouldRejectInvalidOrUsedToken() {
        when(tokenStore.consume("expiredToken", "PASSWORD_RESET")).thenReturn(null);

        assertThatThrownBy(() -> service.resetPassword(
                new ResetPasswordRequest("expiredToken", "NewPassword1", "NewPassword1"), http))
                .isInstanceOf(InvalidResetTokenException.class);
    }

    @Test
    void resetPasswordShouldChangePasswordAndInvalidateOutstanding() {
        when(tokenStore.consume("goodToken", "PASSWORD_RESET")).thenReturn(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewPassword1")).thenReturn("hashed-new");

        service.resetPassword(
                new ResetPasswordRequest("goodToken", "NewPassword1", "NewPassword1"), http);

        verify(passwordEncoder).encode("NewPassword1");
        verify(otpService).invalidateOtpFor(5L, OtpPurpose.PASSWORD_RESET);
        verify(tokenStore).invalidate("PASSWORD_RESET", 5L);
    }

    @Test
    void resetPasswordShouldRejectPasswordEqualToLoginEmail() {
        when(tokenStore.consume("goodToken", "PASSWORD_RESET")).thenReturn(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.resetPassword(
                new ResetPasswordRequest("goodToken", "john@rental.com", "john@rental.com"), http))
                .isInstanceOf(com.example.spring_boot_project_api.exception.InvalidPasswordException.class);
    }

    @Test
    void verifyTwoFactorShouldFailForInvalidChallenge() {
        when(tokenStore.consume("badChallenge", "TWO_FACTOR_CHALLENGE")).thenReturn(null);

        assertThatThrownBy(() -> service.verifyTwoFactor("badChallenge", "123456", http))
                .isInstanceOf(com.example.spring_boot_project_api.exception.TwoFactorChallengeException.class);
    }

    @Test
    void verifyTwoFactorShouldReturnUserIdOnSuccess() {
        when(tokenStore.consume("challenge", "TWO_FACTOR_CHALLENGE")).thenReturn(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        Long userId = service.verifyTwoFactor("challenge", "123456", http);

        assertThat(userId).isEqualTo(5L);
        verify(otpService).verifyOtp(user, OtpPurpose.TWO_FACTOR_LOGIN, "123456");
    }

    @Test
    void enableTwoFactorShouldFailWithoutVerifiedRecovery() {
        UserSecurity noRecovery = UserSecurity.builder()
                .id(1L).user(user).recoveryEmailVerified(false).build();
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(userSecurityRepository.findByUserId(5L)).thenReturn(Optional.of(noRecovery));

        assertThatThrownBy(() -> service.enableTwoFactor(http))
                .isInstanceOf(TwoFactorException.class);
    }

    @Test
    void enableTwoFactorShouldSucceedWithVerifiedRecovery() {
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(userSecurityRepository.findByUserId(5L)).thenReturn(Optional.of(verifiedSecurity()));

        MessageResponse response = service.enableTwoFactor(http);

        assertThat(response.message()).contains("enabled");
        var captor = org.mockito.ArgumentCaptor.forClass(UserSecurity.class);
        verify(userSecurityRepository).save(captor.capture());
        assertThat(captor.getValue().getTwoFactorEnabled()).isTrue();
        assertThat(captor.getValue().getTwoFactorMethod()).isEqualTo(TwoFactorMethod.EMAIL);
    }

    @Test
    void enableTwoFactorShouldSucceedForTenantWithoutRecovery() {
        // Tenant accounts have no separate recovery email; 2FA codes are sent to
        // their primary login email, so no verified recovery is required.
        User tenant = User.builder().id(8L).username("joy").email("joy@gmail.com").build();
        when(securityUtils.getCurrentUser()).thenReturn(tenant);
        when(securityUtils.hasRole(8L, "TENANT")).thenReturn(true);
        when(userSecurityRepository.findByUserId(8L)).thenReturn(Optional.of(
                UserSecurity.builder().id(7L).user(tenant).recoveryEmailVerified(false).build()));

        MessageResponse response = service.enableTwoFactor(http);

        assertThat(response.message()).contains("enabled");
        var captor = org.mockito.ArgumentCaptor.forClass(UserSecurity.class);
        verify(userSecurityRepository).save(captor.capture());
        assertThat(captor.getValue().getTwoFactorEnabled()).isTrue();
    }

    @Test
    void addRecoveryEmailShouldRejectRentalComDomain() {
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(userSecurityRepository.findByUserId(5L)).thenReturn(Optional.of(
                UserSecurity.builder().id(1L).user(user).recoveryEmailVerified(false).build()));

        assertThatThrownBy(() -> service.addRecoveryEmail(
                new AddRecoveryEmailRequest("backup@rental.com"), http))
                .isInstanceOf(RecoveryEmailException.class)
                .hasMessageContaining("external email");
    }

    @Test
    void addRecoveryEmailShouldAcceptExternalEmailForRentalComLogin() {
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(userSecurityRepository.findByUserId(5L)).thenReturn(Optional.of(
                UserSecurity.builder().id(1L).user(user).recoveryEmailVerified(false).build()));
        when(otpService.isWithinResendCooldown(5L, OtpPurpose.EMAIL_VERIFICATION)).thenReturn(false);
        when(otpService.issueOtp(user, OtpPurpose.EMAIL_VERIFICATION)).thenReturn("123456");
        when(userSecurityRepository.save(any(UserSecurity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MessageResponse response = service.addRecoveryEmail(
                new AddRecoveryEmailRequest("john.doe@gmail.com"), http);

        assertThat(response.message()).contains("verification code");
        verify(emailService)
                .sendRecoveryEmailVerificationOtp("john.doe@gmail.com", "123456", 5);
        var captor = org.mockito.ArgumentCaptor.forClass(UserSecurity.class);
        verify(userSecurityRepository).save(captor.capture());
        assertThat(captor.getValue().getPendingRecoveryEmail()).isEqualTo("john.doe@gmail.com");
    }

    @Test
    void addRecoveryEmailShouldAllowGmailWhenLoginEmailIsGmail() {
        User gmailUser = User.builder().id(9L).username("bob").email("bob@gmail.com").build();
        when(securityUtils.getCurrentUser()).thenReturn(gmailUser);
        when(userSecurityRepository.findByUserId(9L)).thenReturn(Optional.of(
                UserSecurity.builder().id(2L).user(gmailUser).recoveryEmailVerified(false).build()));
        when(otpService.isWithinResendCooldown(9L, OtpPurpose.EMAIL_VERIFICATION)).thenReturn(false);
        when(otpService.issueOtp(gmailUser, OtpPurpose.EMAIL_VERIFICATION)).thenReturn("654321");
        when(userSecurityRepository.save(any(UserSecurity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MessageResponse response = service.addRecoveryEmail(
                new AddRecoveryEmailRequest("bob.personal@gmail.com"), http);

        assertThat(response.message()).contains("verification code");
        verify(emailService)
                .sendRecoveryEmailVerificationOtp("bob.personal@gmail.com", "654321", 5);
    }
}
