package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;

public record UtilityUsageResponse(
        String month,
        BigDecimal usage,
        String unit
) {
}