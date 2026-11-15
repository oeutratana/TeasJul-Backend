package com.example.spring_boot_project_api.exception;

public class ListingAmenityNotFoundException extends RuntimeException {

    public ListingAmenityNotFoundException(String message) {
        super(message);
    }

    public static ListingAmenityNotFoundException forIds(Long listingId, Long amenityId) {
        return new ListingAmenityNotFoundException(
                "Amenity " + amenityId + " is not assigned to listing " + listingId);
    }
}