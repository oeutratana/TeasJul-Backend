package com.example.spring_boot_project_api.exception;

public class InvoiceNotFoundException extends RuntimeException {

    public InvoiceNotFoundException(String message) {
        super(message);
    }

    public static InvoiceNotFoundException forId(Long id) {
        return new InvoiceNotFoundException("Invoice not found with id: " + id);
    }
}
