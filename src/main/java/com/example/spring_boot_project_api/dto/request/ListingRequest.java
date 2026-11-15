package com.example.spring_boot_project_api.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.example.spring_boot_project_api.enums.Furnishing;
import com.example.spring_boot_project_api.enums.ListingScope;
import com.example.spring_boot_project_api.enums.PropertyType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Create / update payload for a grouped multi-room listing.
 *
 * <p>Only the house, room type and an optional list of physical rooms are
 * required. Every other characteristic is optional, mirroring the "fall back to
 * house/room-type defaults" behavior of the room model (see the public room
 * queries). An empty/null {@code roomIds} list keeps the currently attached
 * rooms untouched when updating.</p>
 */
public record ListingRequest(

        @NotNull(message = "House ID is required")
        @Positive(message = "House ID must be positive")
        Long houseId,

        @NotNull(message = "Room type ID is required")
        @Positive(message = "Room type ID must be positive")
        Long roomTypeId,

        @Size(max = 120, message = "Title cannot exceed 120 characters")
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

        List<@Positive(message = "Room IDs must be positive") Long> roomIds
) {
}