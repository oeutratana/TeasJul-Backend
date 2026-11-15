package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreatePaymentMethodRequest(
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Code is required") String code,
        String description
) {
}