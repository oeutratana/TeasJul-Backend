package com.example.spring_boot_project_api.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.response.LoginHistoryResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.service.LoginHistoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * SUPER_ADMIN security monitoring. Authorization is enforced server-side in
 * {@code LoginHistoryServiceImpl}, never in the controller.
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "User Security Administration",
        description = "SUPER_ADMIN review of account sign-in history.")
public class AdminLoginHistoryController {

    private final LoginHistoryService loginHistoryService;

    @Operation(summary = "List an account's login history",
            description = "Paginated sign-in attempts (outcome, method, client IP and user agent) for "
                    + "the given user. Requires the SUPER_ADMIN role; the account is resolved by id.")
    @GetMapping("/{id}/login-history")
    public ResponseEntity<PageResponse<LoginHistoryResponse>> getUserLoginHistory(
            @PathVariable Long id,
            @PageableDefault(size = 10, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(loginHistoryService.getHistoryForUser(id, pageable));
    }
}
