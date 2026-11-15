package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDateTime;

import com.example.spring_boot_project_api.enums.UtilityBillingMethod;
import com.example.spring_boot_project_api.enums.UtilityTypeStatus;

/**
 * Shared utility configured at the listing level (mirrors {@link RoomUtilityResponse}).
 */
public record ListingUtilityResponse(
        Long id,
        Long listingId,
        Long utilityTypeId,
        String utilityTypeName,
        String utilityTypeCode,
        UtilityBillingMethod billingMethod,
        UtilityTypeStatus status,
        LocalDateTime createdAt,
        Boolean isIncluded
) {
}