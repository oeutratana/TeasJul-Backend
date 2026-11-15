package com.example.spring_boot_project_api.dto.response;

public record ActiveRoomTenantDto(
        Long roomId,
        Long tenantId,
        String firstName,
        String lastName,
        Boolean isPrimary
) {
}
