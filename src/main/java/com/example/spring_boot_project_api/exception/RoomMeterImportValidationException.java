package com.example.spring_boot_project_api.exception;

import java.util.List;
import java.util.Map;

/**
 * Raised when a rooms + meters import cannot be confirmed because some rows are invalid.
 * Row numbers are 1-based and align with the source file / preview.
 */
public class RoomMeterImportValidationException extends RuntimeException {

    private final Map<Integer, List<String>> rowErrors;

    public RoomMeterImportValidationException(String message, Map<Integer, List<String>> rowErrors) {
        super(message);
        this.rowErrors = rowErrors;
    }

    public Map<Integer, List<String>> getRowErrors() {
        return rowErrors;
    }
}