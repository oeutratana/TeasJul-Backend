package com.example.spring_boot_project_api.dto.request;

import java.math.BigDecimal;

import com.example.spring_boot_project_api.enums.NearbyLocationCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateNearbyLocationRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 200, message = "Name must not exceed 200 characters")
        String name,

        @NotNull(message = "Category is required")
        NearbyLocationCategory category,

        @NotNull(message = "Distance in km is required")
        @PositiveOrZero(message = "Distance in km cannot be negative")
        BigDecimal distanceKm,

        @NotNull(message = "Travel minutes is required")
        @PositiveOrZero(message = "Travel minutes cannot be negative")
        Integer travelMinutes
) {
}