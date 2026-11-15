package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.PaymentResponse;
import com.example.spring_boot_project_api.model.Payment;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getInvoiceId(),
                payment.getPaymentMethod().getId(),
                payment.getPaymentMethod().getName(),
                payment.getPaymentNo(),
                payment.getTransactionId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getExchangeRate(),
                payment.getExchangeRateTo(),
                payment.getStatus(),
                payment.getQrCode(),
                payment.getPaidAt(),
                payment.getCreatedAt());
    }
}
