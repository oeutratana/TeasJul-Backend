package com.example.spring_boot_project_api.exception;

public class PayWayIntegrationException extends RuntimeException {

    private final String payWayCode;

    public PayWayIntegrationException(String message, String payWayCode) {
        super(message);
        this.payWayCode = payWayCode;
    }

    public PayWayIntegrationException(String message, Throwable cause, String payWayCode) {
        super(message, cause);
        this.payWayCode = payWayCode;
    }

    public String getPayWayCode() {
        return payWayCode;
    }
}
