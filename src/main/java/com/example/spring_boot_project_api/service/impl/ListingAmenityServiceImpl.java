package com.example.spring_boot_project_api.service.impl;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.response.ListingAmenityResponse;
import com.example.spring_boot_project_api.exception.AmenityNotFoundException;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.ListingAmenityNotFoundException;
import com.example.spring_boot_project_api.mapper.ListingAmenityMapper;
import com.example.spring_boot_project_api.model.Amenity;
import com.example.spring_boot_project_api.model.Listing;
import com.example.spring_boot_project_api.repository.AmenityRepository;
import com.example.spring_boot_project_api.repository.ListingAmenityRepository;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.service.ListingAccessService;
import com.example.spring_boot_project_api.service.ListingAmenityService;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListingAmenityServiceImpl implements ListingAmenityService {

    private static final String ACCESS_DENIED_MSG =
            "You do not have permission to access this resource";

    private final ListingAmenityRepository listingAmenityRepository;
    private final AmenityRepository amenityRepository;
    private final ListingAccessService listingAccessService;
    private final HouseAccessService houseAccessService;
    private final SecurityUtils securityUtils;
    private final ListingAmenityMapper listingAmenityMapper;

    @Override
    @Transactional
    public ListingAmenityResponse assignAmenity(Long listingId, Long amenityId) {
        Listing listing = listingAccessService.requireListingManage(listingId);

        Long currentUserId = securityUtils.getCurrentUserId();
        Amenity amenity = amenityRepository.findById(amenityId)
                .orElseThrow(() -> AmenityNotFoundException.forId(amenityId));

        if (amenity.getOwnerId() != null
                && !amenity.getOwnerId().equals(currentUserId)
                && !houseAccessService.getScopedOwnerIds().contains(amenity.getOwnerId())) {
            throw new AccessDeniedException(ACCESS_DENIED_MSG);
        }

        if (listingAmenityRepository.existsByIdListingIdAndIdAmenityId(listing.getId(), amenityId)) {
            throw new DuplicateResourceException(
                    "Amenity " + amenityId + " is already assigned to listing " + listingId);
        }

        return listingAmenityMapper.toResponse(listingAmenityRepository.save(
                listingAmenityMapper.toEntity(listing.getId(), amenity)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListingAmenityResponse> getListingAmenities(Long listingId) {
        Listing listing = listingAccessService.requireListingManage(listingId);
        return listingAmenityRepository.findByIdListingId(listing.getId()).stream()
                .map(listingAmenityMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void removeAmenity(Long listingId, Long amenityId) {
        Listing listing = listingAccessService.requireListingManage(listingId);
        if (!listingAmenityRepository.existsByIdListingIdAndIdAmenityId(listing.getId(), amenityId)) {
            throw ListingAmenityNotFoundException.forIds(listingId, amenityId);
        }
        listingAmenityRepository.deleteByIdListingIdAndIdAmenityId(listing.getId(), amenityId);
    }
}