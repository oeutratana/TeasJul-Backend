package com.example.spring_boot_project_api.dto.response;

/**
 * Public-safe contact details for the owner of a published house.
 */
public record PublicOwnerResponse(
        Long id,
        String name,
        String phone,
        String email
) {
}
