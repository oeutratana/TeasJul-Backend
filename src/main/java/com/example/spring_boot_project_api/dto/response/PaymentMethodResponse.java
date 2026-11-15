package com.example.spring_boot_project_api.dto.response;

import com.example.spring_boot_project_api.enums.PaymentMethodStatus;

public record PaymentMethodResponse(
        Long id,
        String name,
        String code,
        String description,
        PaymentMethodStatus status
) {
}