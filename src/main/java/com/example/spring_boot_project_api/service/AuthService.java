package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.request.LoginRequest;
import com.example.spring_boot_project_api.dto.request.RegisterRequest;
import com.example.spring_boot_project_api.dto.response.LoginResponse;
import com.example.spring_boot_project_api.dto.response.TwoFactorChallengeResponse;
import com.example.spring_boot_project_api.dto.response.UserResponse;

import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    /**
     * Authenticates a user. If two-factor authentication is required, no final
     * JWT is issued; instead a {@link TwoFactorChallengeResponse} is returned
     * and authentication completes only after the challenge is verified.
     */
    Object login(LoginRequest request, HttpServletRequest http);

    /**
     * Completes a two-factor login after the challenge token and OTP have been
     * verified, issuing the final JWT.
     */
    LoginResponse completeTwoFactorLogin(String challengeToken, String otp, HttpServletRequest http);
}
