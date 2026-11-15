package com.example.spring_boot_project_api.exception;

/**
 * Thrown when a report query parameter is invalid (e.g. month outside 1-12 or
 * an out-of-range year). Maps to HTTP 400.
 */
public class InvalidReportQueryException extends RuntimeException {

    public InvalidReportQueryException(String message) {
        super(message);
    }
}