package com.example.spring_boot_project_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.AddRecoveryEmailRequest;
import com.example.spring_boot_project_api.dto.request.VerifyRecoveryEmailRequest;
import com.example.spring_boot_project_api.dto.response.MessageResponse;
import com.example.spring_boot_project_api.dto.response.UserSecurityResponse;
import com.example.spring_boot_project_api.service.SecurityService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/security")
@RequiredArgsConstructor
@Tag(name = "Account Security")
public class SecurityController {

    private final SecurityService securityService;

    @GetMapping
    public ResponseEntity<UserSecurityResponse> getMySecurity() {
        return ResponseEntity.ok(securityService.getMySecurity());
    }

    @PostMapping("/recovery-email")
    public ResponseEntity<MessageResponse> addRecoveryEmail(
            @Valid @RequestBody AddRecoveryEmailRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(securityService.addRecoveryEmail(request, http));
    }

    @PostMapping("/recovery-email/verify")
    public ResponseEntity<MessageResponse> verifyRecoveryEmail(
            @Valid @RequestBody VerifyRecoveryEmailRequest request, HttpServletRequest http) {
        return ResponseEntity.ok(securityService.verifyRecoveryEmail(request, http));
    }

    @PostMapping("/2fa/enable")
    public ResponseEntity<MessageResponse> enableTwoFactor(HttpServletRequest http) {
        return ResponseEntity.ok(securityService.enableTwoFactor(http));
    }

    @PostMapping("/2fa/disable")
    public ResponseEntity<MessageResponse> disableTwoFactor(HttpServletRequest http) {
        return ResponseEntity.ok(securityService.disableTwoFactor(http));
    }
}
