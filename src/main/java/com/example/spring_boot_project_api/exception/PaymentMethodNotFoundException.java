package com.example.spring_boot_project_api.exception;

public class PaymentMethodNotFoundException extends RuntimeException {

    public PaymentMethodNotFoundException(String message) {
        super(message);
    }

    public static PaymentMethodNotFoundException forId(Long id) {
        return new PaymentMethodNotFoundException("Payment method not found with id: " + id);
    }
}
