package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.ListingUtilityResponse;
import com.example.spring_boot_project_api.enums.UtilityTypeStatus;
import com.example.spring_boot_project_api.model.ListingUtility;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UtilityType;

@Component
public class ListingUtilityMapper {

    public ListingUtility toEntity(User owner, Long listingId, UtilityType utilityType,
            Boolean isIncluded, UtilityTypeStatus status) {
        return ListingUtility.builder()
                .owner(owner != null ? owner : utilityType.getOwner())
                .listingId(listingId)
                .utilityTypeId(utilityType.getId())
                .status(status != null ? status : UtilityTypeStatus.ACTIVE)
                .isIncluded(Boolean.TRUE.equals(isIncluded))
                .build();
    }

    public ListingUtilityResponse toResponse(ListingUtility listingUtility, UtilityType utilityType) {
        return new ListingUtilityResponse(
                listingUtility.getId(),
                listingUtility.getListingId(),
                utilityType.getId(),
                utilityType.getName(),
                utilityType.getCode(),
                utilityType.getBillingMethod(),
                listingUtility.getStatus(),
                listingUtility.getCreatedAt(),
                listingUtility.getIsIncluded());
    }
}