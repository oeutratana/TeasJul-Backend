package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreatePaymentRequest(
        @NotNull(message = "Invoice ID is required")
        Long invoiceId,

        Long paymentMethodId
) {
}
