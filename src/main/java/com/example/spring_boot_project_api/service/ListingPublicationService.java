package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.enums.RoomPublicationStatus;
import com.example.spring_boot_project_api.model.Listing;

/**
 * Publication workflow for grouped listings (mirrors {@link RoomPublicationService}).
 *
 * <p>Submitting moves the listing — and its attached rooms — to PENDING_APPROVAL;
 * approving publishes the listing AND its rooms together; rejecting or unpublishing
 * reverts the whole group.</p>
 */
public interface ListingPublicationService {

    Listing submitForApproval(Long listingId);

    Listing approve(Long listingId);

    Listing reject(Long listingId, String rejectionReason);

    Listing unpublish(Long listingId);

    RoomPublicationStatus getPublicationStatus(Long listingId);

    /** Role-aware listing for the workflow UI. */
    List<Listing> getListingsByPublicationStatus(RoomPublicationStatus publicationStatus);

    /** SUPER_ADMIN review list (optionally filtered by status). */
    List<Listing> getListingsForReview(RoomPublicationStatus publicationStatus);
}