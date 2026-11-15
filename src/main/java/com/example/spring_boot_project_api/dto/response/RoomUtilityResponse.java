package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDateTime;

import com.example.spring_boot_project_api.enums.UtilityBillingMethod;
import com.example.spring_boot_project_api.enums.UtilityTypeStatus;

public record RoomUtilityResponse(
        Long id,
        Long roomId,
        String roomNumber,
        Long utilityTypeId,
        String utilityTypeName,
        String utilityTypeCode,
        UtilityBillingMethod billingMethod,
        UtilityTypeStatus status,
        LocalDateTime createdAt,
        Boolean isIncluded
) {
}