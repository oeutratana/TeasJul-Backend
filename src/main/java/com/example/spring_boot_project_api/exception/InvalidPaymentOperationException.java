package com.example.spring_boot_project_api.exception;

public class InvalidPaymentOperationException extends RuntimeException {

    public InvalidPaymentOperationException(String message) {
        super(message);
    }
}
