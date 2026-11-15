package com.example.spring_boot_project_api.service;

import org.springframework.data.domain.Pageable;

import com.example.spring_boot_project_api.dto.response.LoginHistoryResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.enums.LoginMethod;
import com.example.spring_boot_project_api.enums.LoginOutcome;
import com.example.spring_boot_project_api.model.User;

import jakarta.servlet.http.HttpServletRequest;

public interface LoginHistoryService {

    /**
     * Appends one sign-in attempt to the account's history.
     *
     * <p>Implemented in its own transaction so a {@code FAILED} record survives
     * the rollback caused by the {@code BadCredentialsException} that the login
     * flow throws immediately afterwards. A {@code null} user records a failed
     * attempt against an unknown identifier.</p>
     */
    void record(User user, LoginOutcome outcome, LoginMethod method, HttpServletRequest request);

    /** The authenticated caller's own history. The owner is resolved server-side. */
    PageResponse<LoginHistoryResponse> getMyHistory(Pageable pageable);

    /** Any account's history. Requires the SUPER_ADMIN role. */
    PageResponse<LoginHistoryResponse> getHistoryForUser(Long userId, Pageable pageable);
}
