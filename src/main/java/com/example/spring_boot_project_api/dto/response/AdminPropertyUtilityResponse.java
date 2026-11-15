package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;

/**
 * A utility assignment with its current billing rate (when not included in the
 * rent), used by the admin posting detail/continue-editing views.
 */
public record AdminPropertyUtilityResponse(
        Long utilityTypeId,
        String utilityTypeName,
        String unit,
        Boolean isIncluded,
        BigDecimal rate) {
}