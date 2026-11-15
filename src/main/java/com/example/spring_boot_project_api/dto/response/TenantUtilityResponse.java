package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TenantUtilityResponse(
        Long meterId,
        String meterNumber,
        String utilityTypeName,
        String unit,
        BigDecimal lastReading,
        BigDecimal lastUsage,
        LocalDate readingDate
) {
}