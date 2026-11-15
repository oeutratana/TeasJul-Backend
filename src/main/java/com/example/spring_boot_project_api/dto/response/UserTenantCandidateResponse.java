package com.example.spring_boot_project_api.dto.response;

/**
 * Lightweight projection of a user returned by the "available for tenant"
 * search selector. Contains only the fields the tenant-linking UI needs and
 * deliberately excludes sensitive data (password hash, tokens, etc.).
 */
public record UserTenantCandidateResponse(
        Long id,
        String username,
        String firstName,
        String lastName,
        String email,
        String phone
) {
}
