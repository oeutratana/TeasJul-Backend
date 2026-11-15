package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.example.spring_boot_project_api.enums.Furnishing;
import com.example.spring_boot_project_api.enums.ListingScope;
import com.example.spring_boot_project_api.enums.PropertyType;
import com.example.spring_boot_project_api.enums.RoomPublicationStatus;

/**
 * Owner/manager view of a grouped multi-room listing, including the currently
 * attached physical rooms, gallery, amenities and utilities.
 */
public record ListingResponse(
        Long id,
        Long ownerId,
        Long houseId,
        String houseName,
        Long roomTypeId,
        String roomTypeName,
        String title,
        String description,
        PropertyType propertyType,
        ListingScope listingScope,
        Furnishing furnishing,
        LocalDate availableFrom,
        BigDecimal advancePayment,
        Integer bedrooms,
        Integer bathrooms,
        BigDecimal areaSqm,
        BigDecimal securityDeposit,
        String blueprintPublicId,
        String blueprintSecureUrl,
        RoomPublicationStatus publicationStatus,
        String rejectionReason,
        LocalDateTime submittedAt,
        LocalDateTime approvedAt,
        LocalDateTime publishedAt,
        LocalDateTime rejectedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ListingRoomResponse> rooms,
        List<ListingImageResponse> images,
        List<ListingAmenityResponse> amenities,
        List<ListingUtilityResponse> utilities
) {
}