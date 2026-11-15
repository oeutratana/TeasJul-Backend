package com.example.spring_boot_project_api.service;

import org.springframework.data.domain.Pageable;

import com.example.spring_boot_project_api.dto.request.UpdateUserRequest;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.UserResponse;
import com.example.spring_boot_project_api.dto.response.UserSummaryResponse;
import com.example.spring_boot_project_api.dto.response.UserTenantCandidateResponse;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserStatus;

public interface UserService {

    User createUser(User user);

    UserResponse getUserById(Long id);

    UserResponse getCurrentUser();

    /**
     * Paginated, server-side search and status filter for all users in the system.
     * A SUPER_ADMIN sees every user; an OWNER only sees the users inside their own
     * ownership scope.
     */
    PageResponse<UserResponse> getAllUsers(String search, UserStatus status, Pageable pageable);

    UserSummaryResponse getUsersSummary();

    UserResponse updateUser(Long id, UpdateUserRequest request);

    void deleteUser(Long id);

    /**
     * Searches users eligible to be linked to a new tenant profile (paginated,
     * server-side). The page size is capped to prevent retrieving the whole table.
     */
    PageResponse<UserTenantCandidateResponse> searchAvailableForTenant(String q, Pageable pageable);

    /**
     * Throws {@code AccessDeniedException} unless the current user may view,
     * update or delete the given user.
     *
     * <p>SUPER_ADMIN is unrestricted. An OWNER is restricted to their own
     * ownership scope (see {@code HouseAccessService}) and can never target
     * themselves-as-another-owner, another OWNER, or any SUPER_ADMIN.
     *
     * <p>Intended for reuse by other services that manage a specific user record
     * on behalf of the current caller (for example role assignment).
     */
    void requireUserManagementAccess(Long userId);
}
