package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.example.spring_boot_project_api.enums.Furnishing;
import com.example.spring_boot_project_api.enums.ListingScope;
import com.example.spring_boot_project_api.enums.PropertyType;

/**
 * Public face of a grouped multi-room listing — the exact shape rendered on the
 * public website and on the owner/manager "preview public listing" page.
 *
 * <p>Contains only data that is safe to expose to anonymous visitors (no owner
 * contact details, tenants or internal business data).</p>
 */
public record PublicListingResponse(
        Long id,
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
        String blueprintSecureUrl,
        LocalDateTime publishedAt,

        Long houseId,
        String houseName,
        String houseDescription,
        String houseAddress,
        String postalCode,
        String cityProvince,
        BigDecimal latitude,
        BigDecimal longitude,
        String googleMapsUrl,

        List<String> houseImages,

        Long roomTypeId,
        String roomTypeName,
        String roomTypeDescription,
        List<String> roomTypeImages,

        List<String> images,

        Long availableRoomCount,
        BigDecimal fromRent,
        BigDecimal maxRent,

        List<PublicListingRoom> rooms,

        List<String> amenities,

        List<PublicUtilityResponse> utilities,

        List<NearbyLocationResponse> nearbyLocations
) {
}