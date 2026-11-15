package com.example.spring_boot_project_api.dto.request;

import com.example.spring_boot_project_api.enums.PaymentMethodStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdatePaymentMethodRequest(
        @NotBlank(message = "Name is required") String name,
        String description,
        @NotNull(message = "Status is required") PaymentMethodStatus status
) {
}