package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.response.ListingUtilityResponse;

/**
 * Listing-level utility management (mirrors {@link RoomUtilityService}).
 */
public interface ListingUtilityService {

    ListingUtilityResponse assignUtility(Long listingId, Long utilityTypeId, Boolean isIncluded);

    List<ListingUtilityResponse> getListingUtilities(Long listingId);

    void removeUtility(Long listingId, Long utilityTypeId);
}