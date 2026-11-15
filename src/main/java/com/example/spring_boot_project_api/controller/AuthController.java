package com.example.spring_boot_project_api.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.ForgotPasswordRequest;
import com.example.spring_boot_project_api.dto.request.LoginRequest;
import com.example.spring_boot_project_api.dto.request.RegisterRequest;
import com.example.spring_boot_project_api.dto.request.ResetPasswordRequest;
import com.example.spring_boot_project_api.dto.request.VerifyPasswordResetOtpRequest;
import com.example.spring_boot_project_api.dto.request.VerifyTwoFactorRequest;
import com.example.spring_boot_project_api.dto.response.LoginHistoryResponse;
import com.example.spring_boot_project_api.dto.response.LoginResponse;
import com.example.spring_boot_project_api.dto.response.MessageResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.ResetOtpVerificationResponse;
import com.example.spring_boot_project_api.dto.response.TwoFactorChallengeResponse;
import com.example.spring_boot_project_api.dto.response.UserResponse;
import com.example.spring_boot_project_api.service.AuthService;
import com.example.spring_boot_project_api.service.LoginHistoryService;
import com.example.spring_boot_project_api.service.SecurityService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;
    private final SecurityService securityService;
    private final LoginHistoryService loginHistoryService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse createdUser = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest http) {
        return ResponseEntity.ok(authService.login(request, http));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(securityService.forgotPassword(request, http));
    }

    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<ResetOtpVerificationResponse> verifyPasswordResetOtp(
            @Valid @RequestBody VerifyPasswordResetOtpRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(securityService.verifyPasswordResetOtp(request, http));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(securityService.resetPassword(request, http));
    }

    /**
     * Completes 2FA login. Only a valid challenge token (issued after correct
     * credentials) plus the correct OTP results in a final JWT.
     */
    @PostMapping("/2fa/verify")
    public ResponseEntity<LoginResponse> verifyTwoFactor(
            @Valid @RequestBody VerifyTwoFactorRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(
                authService.completeTwoFactorLogin(request.challengeToken(), request.otp(), http));
    }

    /**
     * The caller's own sign-in history. JWT required (this path is not part of
     * the permitAll list). The owner is resolved from the Spring Security
     * context inside the service, so no user id is accepted from the request
     * and a caller can never read another account's history here.
     */
    @GetMapping("/login-history")
    public ResponseEntity<PageResponse<LoginHistoryResponse>> getMyLoginHistory(
            @PageableDefault(size = 10, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(loginHistoryService.getMyHistory(pageable));
    }
}
