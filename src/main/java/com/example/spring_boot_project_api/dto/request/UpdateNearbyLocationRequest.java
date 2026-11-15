package com.example.spring_boot_project_api.dto.request;

import java.math.BigDecimal;

import com.example.spring_boot_project_api.enums.NearbyLocationCategory;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateNearbyLocationRequest(

        @Size(max = 200, message = "Name must not exceed 200 characters")
        String name,

        NearbyLocationCategory category,

        @PositiveOrZero(message = "Distance in km cannot be negative")
        BigDecimal distanceKm,

        @PositiveOrZero(message = "Travel minutes cannot be negative")
        Integer travelMinutes
) {
}