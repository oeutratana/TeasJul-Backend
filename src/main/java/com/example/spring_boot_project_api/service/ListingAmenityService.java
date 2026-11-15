package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.response.ListingAmenityResponse;

/**
 * Listing-level amenity management (mirrors {@link RoomAmenityService}).
 */
public interface ListingAmenityService {

    ListingAmenityResponse assignAmenity(Long listingId, Long amenityId);

    List<ListingAmenityResponse> getListingAmenities(Long listingId);

    void removeAmenity(Long listingId, Long amenityId);
}