package com.example.spring_boot_project_api.service;

import org.springframework.data.domain.Pageable;

import com.example.spring_boot_project_api.dto.request.CreateOwnerRequest;
import com.example.spring_boot_project_api.dto.request.UpdateUserRequest;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.UserResponse;
import com.example.spring_boot_project_api.model.UserStatus;

/**
 * SUPER_ADMIN management of OWNER accounts. Every operation is restricted to
 * the SUPER_ADMIN role and enforced server-side.
 */
public interface OwnerAdminService {

    UserResponse createOwner(CreateOwnerRequest request);

    PageResponse<UserResponse> listOwners(String search, UserStatus status, Pageable pageable);

    UserResponse getOwner(Long id);

    UserResponse updateOwner(Long id, UpdateUserRequest request);

    UserResponse updateOwnerStatus(Long id, UserStatus status);
}