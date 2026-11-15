package com.example.spring_boot_project_api.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record ContractTenantRequest(
        @NotNull(message = "Contract ID is required")
        Long contractId,

        @NotNull(message = "Tenant ID is required")
        Long tenantId,

        Boolean isPrimary,

        @NotNull(message = "Joined date is required")
        LocalDate joinedAt,

        LocalDate leftAt) {
}
