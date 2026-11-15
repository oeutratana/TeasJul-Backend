package com.example.spring_boot_project_api.service.impl;

import java.time.LocalDateTime;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.LoginRequest;
import com.example.spring_boot_project_api.dto.request.RegisterRequest;
import com.example.spring_boot_project_api.dto.response.LoginResponse;
import com.example.spring_boot_project_api.dto.response.UserResponse;
import com.example.spring_boot_project_api.enums.LoginMethod;
import com.example.spring_boot_project_api.enums.LoginOutcome;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.InvalidEmailException;
import com.example.spring_boot_project_api.mapper.UserMapper;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserStatus;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.AuthService;
import com.example.spring_boot_project_api.service.LoginHistoryService;
import com.example.spring_boot_project_api.service.UserService;
import com.example.spring_boot_project_api.util.EmailValidator;
import com.example.spring_boot_project_api.util.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final SecurityServiceImpl securityService;
    private final LoginHistoryService loginHistoryService;
    private final SecurityAuditLogger audit;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email is already registered");
        }

        // Self-registration is an internal (non-TENANT) account path: tenants join
        // via invitation acceptance and may use any login email, while internal
        // accounts require a corporate-domain login email.
        if (!EmailValidator.isOnInternalDomain(request.email())) {
            throw new InvalidEmailException(EmailValidator.internalEmailRequirement());
        }

        User user = userMapper.toEntity(request);
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        User savedUser = userService.createUser(user);
        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional
    public Object login(LoginRequest request, HttpServletRequest http) {
        User user;
        try {
            user = authenticate(request.usernameOrEmail(), request.password());
        } catch (RuntimeException ex) {
            // A rejected attempt is still a security signal worth keeping. The
            // user is attached only when the identifier resolved to an account,
            // so probing unknown usernames is recorded too. No credential is
            // ever part of the record.
            User rejected = resolveUserQuietly(request.usernameOrEmail());
            loginHistoryService.record(rejected, LoginOutcome.FAILED, LoginMethod.PASSWORD, http);
            audit.log(rejected == null ? null : rejected.getId(), "LOGIN_FAILED", http,
                    "method=PASSWORD reason=" + ex.getClass().getSimpleName());
            throw ex;
        }

        if (securityService.requiresTwoFactor(user)) {
            // Authentication is not complete yet: no SUCCESS record and no
            // lastLoginAt update until the challenge is verified.
            return securityService.createTwoFactorChallenge(user, http);
        }

        user.setLastLoginAt(LocalDateTime.now());
        user = userService.createUser(user);
        loginHistoryService.record(user, LoginOutcome.SUCCESS, LoginMethod.PASSWORD, http);
        audit.log(user.getId(), "LOGIN_SUCCESS", http, "method=PASSWORD");

        String token = jwtUtil.generateToken(user.getUsername());
        return new LoginResponse(token, "Bearer", jwtUtil.getExpirationMs(),
                userMapper.toResponse(user));
    }

    @Override
    @Transactional
    public LoginResponse completeTwoFactorLogin(String challengeToken, String otp,
            HttpServletRequest http) {
        Long userId = securityService.verifyTwoFactor(challengeToken, otp, http);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        user.setLastLoginAt(LocalDateTime.now());
        user = userService.createUser(user);
        loginHistoryService.record(user, LoginOutcome.SUCCESS, LoginMethod.PASSWORD_2FA, http);
        audit.log(user.getId(), "LOGIN_SUCCESS", http, "method=PASSWORD_2FA");

        String token = jwtUtil.generateToken(user.getUsername());
        return new LoginResponse(token, "Bearer", jwtUtil.getExpirationMs(),
                userMapper.toResponse(user));
    }

    private User authenticate(String usernameOrEmail, String rawPassword) {
        User user = findUserByUsernameOrEmail(usernameOrEmail);

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new LockedException("Account is locked or inactive");
        }
        return user;
    }

    /**
     * Resolves the account behind a failed sign-in without ever throwing, so the
     * failure path is not replaced by a lookup error. Returns {@code null} when
     * the identifier matches no account.
     */
    private User resolveUserQuietly(String usernameOrEmail) {
        if (usernameOrEmail == null || usernameOrEmail.isBlank()) {
            return null;
        }
        try {
            return userRepository.findByUsername(usernameOrEmail)
                    .or(() -> userRepository.findByEmail(usernameOrEmail))
                    .orElse(null);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private User findUserByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
    }
}
