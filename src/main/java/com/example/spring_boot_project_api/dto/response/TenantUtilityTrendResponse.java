package com.example.spring_boot_project_api.dto.response;

import java.util.List;

public record TenantUtilityTrendResponse(
        List<UtilityUsageResponse> water,
        List<UtilityUsageResponse> electricity
) {
}