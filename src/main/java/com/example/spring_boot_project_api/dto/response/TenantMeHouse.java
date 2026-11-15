package com.example.spring_boot_project_api.dto.response;

public record TenantMeHouse(
        Long id,
        String name,
        String code,
        String address
) {
}