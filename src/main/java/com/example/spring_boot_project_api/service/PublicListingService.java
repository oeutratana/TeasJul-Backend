package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.response.PublicListingResponse;

/**
 * Public face of grouped listings.
 *
 * <p>No authentication for {@link #getPublicListing}: only listings whose
 * publication status is {@code PUBLISHED} are returned. The preview variant in
 * {@link #getPreview} is authenticated and renders any status for the listing
 * builder's "Preview Public Listing" step.</p>
 */
public interface PublicListingService {

    PublicListingResponse getPublicListing(Long listingId);

    PublicListingResponse getPreview(Long listingId);
}