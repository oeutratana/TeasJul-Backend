package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Assignment payload for a listing-level utility.
 */
public record ListingUtilityRequest(
        @NotNull(message = "Utility type ID is required")
        Long utilityTypeId,

        Boolean isIncluded
) {
}