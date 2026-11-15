package com.example.spring_boot_project_api.dto.request;

import java.math.BigDecimal;

import com.example.spring_boot_project_api.model.MaintenanceCostType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateMaintenanceCostRequest(

        MaintenanceCostType costType,

        @NotBlank(message = "Description is required")
        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description,

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than 0")
        BigDecimal quantity,

        @NotNull(message = "Unit price is required")
        @PositiveOrZero(message = "Unit price must be greater than or equal to 0")
        @DecimalMin(value = "0.00", message = "Unit price must be greater than or equal to 0")
        BigDecimal unitPrice
) {
}
