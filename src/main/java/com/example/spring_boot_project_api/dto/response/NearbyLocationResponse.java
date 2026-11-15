package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;

import com.example.spring_boot_project_api.enums.NearbyLocationCategory;

public record NearbyLocationResponse(
        Long id,
        Long houseId,
        String name,
        NearbyLocationCategory category,
        BigDecimal distanceKm,
        Integer travelMinutes
) {
}