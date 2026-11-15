package com.example.spring_boot_project_api.exception;

public class InvalidInvoiceOperationException extends RuntimeException {

    public InvalidInvoiceOperationException(String message) {
        super(message);
    }
}
