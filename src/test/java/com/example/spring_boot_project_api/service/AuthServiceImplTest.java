package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.spring_boot_project_api.dto.request.LoginRequest;
import com.example.spring_boot_project_api.dto.request.RegisterRequest;
import com.example.spring_boot_project_api.dto.response.LoginResponse;
import com.example.spring_boot_project_api.dto.response.TwoFactorChallengeResponse;
import com.example.spring_boot_project_api.dto.response.UserResponse;
import com.example.spring_boot_project_api.enums.LoginMethod;
import com.example.spring_boot_project_api.enums.LoginOutcome;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.InvalidEmailException;
import com.example.spring_boot_project_api.mapper.UserMapper;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserStatus;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.impl.AuthServiceImpl;
import com.example.spring_boot_project_api.service.impl.SecurityAuditLogger;
import com.example.spring_boot_project_api.service.impl.SecurityServiceImpl;
import com.example.spring_boot_project_api.util.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String RAW_PASSWORD = "Password123";
    private static final String HASHED_PASSWORD = "$2a$10$hashedpasswordhashedsome";

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private SecurityServiceImpl securityService;

    @Mock
    private LoginHistoryService loginHistoryService;

    @Mock
    private SecurityAuditLogger audit;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Spy
    private UserMapper userMapper = new UserMapper();

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest() {
        return new RegisterRequest("johndoe", "john@rental.com", RAW_PASSWORD,
                "John", "Doe", "+1234567890");
    }

    private RegisterRequest registerRequestExternalEmail() {
        return new RegisterRequest("johndoe", "john@gmail.com", RAW_PASSWORD,
                "John", "Doe", "+1234567890");
    }

    private User savedUser() {
        return User.builder()
                .id(1L)
                .username("johndoe")
                .email("john@rental.com")
                .passwordHash(HASHED_PASSWORD)
                .firstName("John")
                .lastName("Doe")
                .phone("+1234567890")
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void registerShouldEncodePasswordAndReturnCreatedUser() {
        RegisterRequest request = registerRequest();
        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.existsByEmail("john@rental.com")).thenReturn(false);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(HASHED_PASSWORD);
        when(userService.createUser(any(User.class))).thenReturn(savedUser());

        UserResponse response = authService.register(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("johndoe");
        assertThat(response.email()).isEqualTo("john@rental.com");
        assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);

        verify(passwordEncoder).encode(RAW_PASSWORD);
        verify(userService).createUser(any(User.class));
    }

    @Test
    void registerShouldRejectDuplicateUsername() {
        when(userRepository.existsByUsername("johndoe")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username is already taken");
    }

    @Test
    void registerShouldRejectDuplicateEmail() {
        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.existsByEmail("john@rental.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email is already registered");
    }

    @Test
    void registerShouldRejectInternalUserWithExternalEmail() {
        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.existsByEmail("john@gmail.com")).thenReturn(false);

        assertThatThrownBy(() -> authService.register(registerRequestExternalEmail()))
                .isInstanceOf(InvalidEmailException.class)
                .hasMessageContaining("@rental.com");
    }

    @Test
    void loginShouldReturnTokenAndRecordLastLogin() {
        LoginRequest request = new LoginRequest("johndoe", RAW_PASSWORD);
        User user = savedUser();
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
        when(securityService.requiresTwoFactor(any(User.class))).thenReturn(false);
        when(jwtUtil.generateToken("johndoe")).thenReturn("jwt-token-value");
        when(jwtUtil.getExpirationMs()).thenReturn(900000L);
        when(userService.createUser(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoginResponse response = (LoginResponse) authService.login(request, httpServletRequest);

        assertThat(response.token()).isEqualTo("jwt-token-value");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900000L);
        assertThat(response.user().username()).isEqualTo("johndoe");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userService).createUser(captor.capture());
        assertThat(captor.getValue().getLastLoginAt()).isNotNull();
    }

    @Test
    void loginByEmailShouldSucceed() {
        LoginRequest request = new LoginRequest("john@example.com", RAW_PASSWORD);
        when(userRepository.findByUsername("john@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(savedUser()));
        when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
        when(securityService.requiresTwoFactor(any(User.class))).thenReturn(false);
        when(jwtUtil.generateToken("johndoe")).thenReturn("jwt-token-value");
        when(jwtUtil.getExpirationMs()).thenReturn(900000L);
        when(userService.createUser(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoginResponse response = (LoginResponse) authService.login(request, httpServletRequest);

        assertThat(response.token()).isEqualTo("jwt-token-value");
    }

    @Test
    void loginShouldFailWithInvalidPassword() {
        LoginRequest request = new LoginRequest("johndoe", "WrongPassword1");
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(savedUser()));
        when(passwordEncoder.matches("WrongPassword1", HASHED_PASSWORD)).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request, httpServletRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    void loginShouldFailWithUnknownUser() {
        LoginRequest request = new LoginRequest("ghost", RAW_PASSWORD);
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request, httpServletRequest))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void loginShouldFailWhenAccountIsInactive() {
        LoginRequest request = new LoginRequest("johndoe", RAW_PASSWORD);
        User user = savedUser();
        user.setStatus(UserStatus.INACTIVE);
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request, httpServletRequest))
                .isInstanceOf(LockedException.class)
                .hasMessageContaining("Account is locked or inactive");
        // last login is never recorded for a deactivated account
        verify(userService, org.mockito.Mockito.never()).createUser(any(User.class));
    }

    // ----- Login history -----

    @Test
    void successfulPasswordLoginShouldRecordSuccessHistory() {
        LoginRequest request = new LoginRequest("johndoe", RAW_PASSWORD);
        User user = savedUser();
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
        when(securityService.requiresTwoFactor(any(User.class))).thenReturn(false);
        when(jwtUtil.generateToken("johndoe")).thenReturn("jwt-token-value");
        when(jwtUtil.getExpirationMs()).thenReturn(900000L);
        when(userService.createUser(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.login(request, httpServletRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(loginHistoryService).record(userCaptor.capture(),
                eq(LoginOutcome.SUCCESS), eq(LoginMethod.PASSWORD), eq(httpServletRequest));
        assertThat(userCaptor.getValue().getId()).isEqualTo(1L);
    }

    @Test
    void failedPasswordLoginShouldRecordFailedHistoryForTheAccount() {
        LoginRequest request = new LoginRequest("johndoe", "WrongPassword1");
        User user = savedUser();
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword1", HASHED_PASSWORD)).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request, httpServletRequest))
                .isInstanceOf(BadCredentialsException.class);

        verify(loginHistoryService).record(sameUser(user), eq(LoginOutcome.FAILED),
                eq(LoginMethod.PASSWORD), eq(httpServletRequest));
        // a rejected attempt never becomes a sign-in
        verify(userService, never()).createUser(any(User.class));
    }

    @Test
    void failedLoginForUnknownAccountShouldStillRecordFailedHistoryWithoutUser() {
        LoginRequest request = new LoginRequest("ghost", RAW_PASSWORD);
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request, httpServletRequest))
                .isInstanceOf(BadCredentialsException.class);

        verify(loginHistoryService).record(isNull(), eq(LoginOutcome.FAILED),
                eq(LoginMethod.PASSWORD), eq(httpServletRequest));
    }

    @Test
    void twoFactorRequiredPasswordLoginShouldNotRecordSuccessOrLastLogin() {
        LoginRequest request = new LoginRequest("johndoe", RAW_PASSWORD);
        User user = savedUser();
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
        when(securityService.requiresTwoFactor(any(User.class))).thenReturn(true);
        when(securityService.createTwoFactorChallenge(any(User.class), any()))
                .thenReturn(new TwoFactorChallengeResponse(true, "challenge-token", 300L, "EMAIL"));

        Object result = authService.login(request, httpServletRequest);

        assertThat(result).isInstanceOf(TwoFactorChallengeResponse.class);
        // the challenge is not a completed sign-in
        verify(loginHistoryService, never()).record(any(), any(), any(), any());
        verifyNoInteractions(jwtUtil);
        verify(userService, never()).createUser(any(User.class));
    }

    @Test
    void successfulTwoFactorLoginShouldRecordSuccessWithTwoFactorMethod() {
        User user = savedUser();
        when(securityService.verifyTwoFactor(anyString(), anyString(), any()))
                .thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken("johndoe")).thenReturn("jwt-token-value");
        when(jwtUtil.getExpirationMs()).thenReturn(900000L);
        when(userService.createUser(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoginResponse response = authService.completeTwoFactorLogin("challenge-token", "123456",
                httpServletRequest);

        assertThat(response.token()).isEqualTo("jwt-token-value");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(loginHistoryService).record(userCaptor.capture(), eq(LoginOutcome.SUCCESS),
                eq(LoginMethod.PASSWORD_2FA), eq(httpServletRequest));
        assertThat(userCaptor.getValue().getLastLoginAt()).isNotNull();
    }

    @Test
    void lastLoginAtShouldOnlyBeStampedOnceAuthenticationCompletes() {
        LoginRequest request = new LoginRequest("johndoe", RAW_PASSWORD);
        User user = savedUser();
        user.setLastLoginAt(null);
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
        when(securityService.requiresTwoFactor(any(User.class))).thenReturn(true);
        when(securityService.createTwoFactorChallenge(any(User.class), any()))
                .thenReturn(new TwoFactorChallengeResponse(true, "challenge-token", 300L, "EMAIL"));

        authService.login(request, httpServletRequest);

        // still untouched while the 2FA challenge is outstanding
        assertThat(user.getLastLoginAt()).isNull();
    }

    @Test
    void loginHistoryShouldNeverReceiveCredentialMaterial() {
        LoginRequest request = new LoginRequest("johndoe", "WrongPassword1");
        User user = savedUser();
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword1", HASHED_PASSWORD)).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request, httpServletRequest))
                .isInstanceOf(BadCredentialsException.class);

        // The recorder's only inputs are the user, the outcome, the method and
        // the request. The submitted password never reaches it, and a failed
        // attempt is never mislabelled as a two-factor sign-in.
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<LoginOutcome> outcomeCaptor = ArgumentCaptor.forClass(LoginOutcome.class);
        verify(loginHistoryService).record(userCaptor.capture(), outcomeCaptor.capture(),
                eq(LoginMethod.PASSWORD), eq(httpServletRequest));
        assertThat(outcomeCaptor.getValue()).isEqualTo(LoginOutcome.FAILED);
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo(HASHED_PASSWORD);
    }

    private static User sameUser(User expected) {
        return org.mockito.ArgumentMatchers.argThat(actual ->
                actual != null && actual.getId().equals(expected.getId()));
    }
}
