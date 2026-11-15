package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;

import com.example.spring_boot_project_api.enums.UtilityBillingMethod;

/** A utility available in a public room listing. */
public record PublicUtilityResponse(
        Long id,
        String name,
        String unit,
        UtilityBillingMethod billingMethod,
        BigDecimal rate,
        Boolean isIncluded
) {
}
