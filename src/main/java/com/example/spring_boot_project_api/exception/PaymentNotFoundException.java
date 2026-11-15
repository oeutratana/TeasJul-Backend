package com.example.spring_boot_project_api.exception;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String message) {
        super(message);
    }

    public static PaymentNotFoundException forId(Long id) {
        return new PaymentNotFoundException("Payment not found with id: " + id);
    }

    public static PaymentNotFoundException forTransactionId(String transactionId) {
        return new PaymentNotFoundException("Payment not found with transaction ID: " + transactionId);
    }
}
