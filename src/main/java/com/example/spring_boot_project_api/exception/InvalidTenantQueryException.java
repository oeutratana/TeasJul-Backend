package com.example.spring_boot_project_api.exception;

/**
 * Thrown when a tenant-provided query parameter is invalid (e.g. malformed
 * month, inverted from/to range, out-of-range pagination). Maps to HTTP 400.
 */
public class InvalidTenantQueryException extends RuntimeException {

    public InvalidTenantQueryException(String message) {
        super(message);
    }
}