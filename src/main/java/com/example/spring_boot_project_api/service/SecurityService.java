package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.request.AddRecoveryEmailRequest;
import com.example.spring_boot_project_api.dto.request.ForgotPasswordRequest;
import com.example.spring_boot_project_api.dto.request.ResetPasswordRequest;
import com.example.spring_boot_project_api.dto.request.VerifyPasswordResetOtpRequest;
import com.example.spring_boot_project_api.dto.request.VerifyRecoveryEmailRequest;
import com.example.spring_boot_project_api.dto.response.MessageResponse;
import com.example.spring_boot_project_api.dto.response.ResetOtpVerificationResponse;
import com.example.spring_boot_project_api.dto.response.UserSecurityResponse;

import jakarta.servlet.http.HttpServletRequest;

public interface SecurityService {

    UserSecurityResponse getMySecurity();

    MessageResponse addRecoveryEmail(AddRecoveryEmailRequest request, HttpServletRequest http);

    MessageResponse verifyRecoveryEmail(VerifyRecoveryEmailRequest request, HttpServletRequest http);

    MessageResponse forgotPassword(ForgotPasswordRequest request, HttpServletRequest http);

    ResetOtpVerificationResponse verifyPasswordResetOtp(VerifyPasswordResetOtpRequest request,
            HttpServletRequest http);

    MessageResponse resetPassword(ResetPasswordRequest request, HttpServletRequest http);

    MessageResponse enableTwoFactor(HttpServletRequest http);

    MessageResponse disableTwoFactor(HttpServletRequest http);
}
