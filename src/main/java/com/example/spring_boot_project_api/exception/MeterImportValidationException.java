package com.example.spring_boot_project_api.exception;

import java.util.List;
import java.util.Map;

/**
 * Raised when a utility meter import cannot be confirmed because some rows are invalid.
 * Row numbers are 1-based and align with the source file / preview.
 */
public class MeterImportValidationException extends RuntimeException {

    private final Map<Integer, List<String>> rowErrors;

    public MeterImportValidationException(String message, Map<Integer, List<String>> rowErrors) {
        super(message);
        this.rowErrors = rowErrors;
    }

    public Map<Integer, List<String>> getRowErrors() {
        return rowErrors;
    }
}