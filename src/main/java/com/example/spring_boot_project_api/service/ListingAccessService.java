package com.example.spring_boot_project_api.service;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.exception.ListingNotFoundException;
import com.example.spring_boot_project_api.model.Listing;
import com.example.spring_boot_project_api.repository.ListingRepository;
import com.example.spring_boot_project_api.util.PermissionConstants;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

/**
 * Centralized authorization helper for grouped listings.
 *
 * <p>An authenticated user may access a listing only when {@link HouseAccessService}
 * grants access to the listing's house. Owner-only operations additionally require
 * the current user to be the house owner (via {@code requireOwnerHouseAccess}).</p>
 */
@Component
@RequiredArgsConstructor
public class ListingAccessService {

    private final ListingRepository listingRepository;
    private final HouseAccessService houseAccessService;
    private final SecurityUtils securityUtils;

    /** Read access: {@code LISTING_VIEW} permission + access to the listing's house. */
    public Listing requireListingView(Long listingId) {
        securityUtils.requirePermission(PermissionConstants.LISTING_VIEW);
        return requireListingAccess(listingId);
    }

    /** Write access to content: {@code LISTING_MANAGE} + access to the listing's house. */
    public Listing requireListingManage(Long listingId) {
        securityUtils.requirePermission(PermissionConstants.LISTING_MANAGE);
        return requireListingAccess(listingId);
    }

    /** Owner-only write access: {@code LISTING_MANAGE} + current user owns the house. */
    public Listing requireOwnerListingManage(Long listingId) {
        securityUtils.requirePermission(PermissionConstants.LISTING_MANAGE);
        Listing listing = getListing(listingId);
        houseAccessService.requireOwnerHouseAccess(listing.getHouseId());
        return listing;
    }

    public Listing requireListingAccess(Long listingId) {
        Listing listing = getListing(listingId);
        houseAccessService.requireHouseAccess(listing.getHouseId());
        return listing;
    }

    public Listing getListing(Long listingId) {
        return listingRepository.findById(listingId)
                .orElseThrow(() -> ListingNotFoundException.forId(listingId));
    }
}