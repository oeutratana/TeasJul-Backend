package com.example.spring_boot_project_api.exception;

public class ListingNotFoundException extends RuntimeException {

    public ListingNotFoundException(String message) {
        super(message);
    }

    public static ListingNotFoundException forId(Long id) {
        return new ListingNotFoundException("Listing not found with id: " + id);
    }
}