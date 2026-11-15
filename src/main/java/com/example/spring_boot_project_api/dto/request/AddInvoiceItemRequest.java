package com.example.spring_boot_project_api.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddInvoiceItemRequest(
        @NotBlank(message = "Description is required")
        @Size(max = 255, message = "Description must be at most 255 characters")
        String description,

        com.example.spring_boot_project_api.enums.InvoiceItemType itemType,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0", message = "Quantity must not be negative")
        BigDecimal quantity,

        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.0", message = "Unit price must not be negative")
        BigDecimal unitPrice
) {
}
