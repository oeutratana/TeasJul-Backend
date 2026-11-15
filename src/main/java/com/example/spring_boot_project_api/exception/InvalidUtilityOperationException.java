package com.example.spring_boot_project_api.exception;

public class InvalidUtilityOperationException extends RuntimeException {

    public InvalidUtilityOperationException(String message) {
        super(message);
    }
}
