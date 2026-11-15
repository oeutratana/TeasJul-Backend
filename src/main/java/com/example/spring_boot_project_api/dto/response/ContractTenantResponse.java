package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ContractTenantResponse(
        Long id,
        Long contractId,
        Long tenantId,
        Boolean isPrimary,
        LocalDate joinedAt,
        LocalDate leftAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
