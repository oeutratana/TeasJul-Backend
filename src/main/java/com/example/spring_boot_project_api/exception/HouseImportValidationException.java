package com.example.spring_boot_project_api.exception;

import java.util.Map;

/**
 * Raised when an existing-house import cannot be confirmed because some rows are invalid.
 * Row numbers are 1-based and align with the source file / preview.
 */
public class HouseImportValidationException extends RuntimeException {

    private final Map<Integer, java.util.List<String>> rowErrors;

    public HouseImportValidationException(String message, Map<Integer, java.util.List<String>> rowErrors) {
        super(message);
        this.rowErrors = rowErrors;
    }

    public Map<Integer, java.util.List<String>> getRowErrors() {
        return rowErrors;
    }
}