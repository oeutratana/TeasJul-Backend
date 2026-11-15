package com.example.spring_boot_project_api.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record RecordManualPaymentRequest(
        @NotNull(message = "Invoice id is required") Long invoiceId,

        @NotNull(message = "Payment method id is required") Long paymentMethodId,

        @DecimalMin(value = "0.01", message = "Amount must be greater than zero") BigDecimal amount,

        LocalDateTime paidAt
) {
}