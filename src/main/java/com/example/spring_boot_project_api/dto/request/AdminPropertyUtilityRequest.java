package com.example.spring_boot_project_api.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * A utility assignment for an admin-posted property. When {@code rate} is
 * provided a current {@code utility_rates} row is also created for the
 * property's house so billing can resolve a price.
 */
@Data
public class AdminPropertyUtilityRequest {

    @NotNull(message = "Utility type is required")
    private Long utilityTypeId;

    private Boolean isIncluded = false;

    private BigDecimal rate;
}