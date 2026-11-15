package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.ListingAmenityResponse;
import com.example.spring_boot_project_api.model.Amenity;
import com.example.spring_boot_project_api.model.ListingAmenity;
import com.example.spring_boot_project_api.model.ListingAmenityId;

@Component
public class ListingAmenityMapper {

    public ListingAmenity toEntity(Long listingId, Amenity amenity) {
        return ListingAmenity.builder()
                .id(new ListingAmenityId(listingId, amenity.getId()))
                .amenity(amenity)
                .build();
    }

    public ListingAmenityResponse toResponse(ListingAmenity listingAmenity) {
        Amenity amenity = listingAmenity.getAmenity();
        return new ListingAmenityResponse(
                listingAmenity.getId().getListingId(),
                amenity.getId(),
                amenity.getName(),
                amenity.getCode(),
                listingAmenity.getCreatedAt());
    }
}