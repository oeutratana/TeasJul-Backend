package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDateTime;

import com.example.spring_boot_project_api.enums.LoginMethod;
import com.example.spring_boot_project_api.enums.LoginOutcome;

/**
 * A single sign-in attempt as returned to the account owner (and to a
 * SUPER_ADMIN reviewing the same account). Contains no secret material.
 */
public record LoginHistoryResponse(
        Long id,
        LocalDateTime occurredAt,
        LoginOutcome outcome,
        LoginMethod method,
        String ip,
        String userAgent
) {
}
