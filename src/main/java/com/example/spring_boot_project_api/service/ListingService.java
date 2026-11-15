package com.example.spring_boot_project_api.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.example.spring_boot_project_api.dto.request.ListingRequest;
import com.example.spring_boot_project_api.dto.response.ListingBlueprintResponse;
import com.example.spring_boot_project_api.dto.response.ListingResponse;
import com.example.spring_boot_project_api.dto.response.ListingRoomResponse;
import com.example.spring_boot_project_api.model.Listing;

/**
 * Owner/manager business operations for grouped multi-room listings.
 */
public interface ListingService {

    /** Returns every listing visible to the current user (own listings / managed houses / all for SUPER_ADMIN). */
    List<ListingResponse> getMyListings();

    ListingResponse getListing(Long listingId);

    /** Finds the listing for the given house + room type, or creates a DRAFT one (OWNER only). */
    ListingResponse getOrCreate(Long houseId, Long roomTypeId);

    /** Returns rooms eligible to be attached to the listing (same house + room type, AVAILABLE). */
    List<ListingRoomResponse> getEligibleRooms(Long listingId);

    ListingResponse create(ListingRequest request);

    ListingResponse update(Long listingId, ListingRequest request);

    void delete(Long listingId);

    ListingBlueprintResponse getBlueprint(Long listingId);

    ListingBlueprintResponse uploadBlueprint(Long listingId, MultipartFile file);

    void deleteBlueprint(Long listingId);

    /** Aggregates an entity into its full response (used by publication endpoints). */
    ListingResponse toResponse(Listing listing);
}