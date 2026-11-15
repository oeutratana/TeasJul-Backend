package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.example.spring_boot_project_api.enums.Furnishing;
import com.example.spring_boot_project_api.enums.ListingScope;
import com.example.spring_boot_project_api.enums.PropertyType;
import com.example.spring_boot_project_api.enums.RoomStatus;

/**
 * Public (anonymous) view of a published room used by the public website.
 *
 * <p>Contains only data that is safe to disclose to visitors: room facts,
 * house name/address, room type name, listing images, amenities, and a limited
 * owner contact. Never includes tenants, contract, payment, credential, or
 * private account data.</p>
 *
 * @param images primary/room-type images merged with house images (URL strings)
 * @param amenities human-readable amenity names
 * @param availableRooms physical rooms currently available in this house and room-type group
 * @param owner public contact for the house (id, name, phone, and email only)
 */
public record PublicRoomResponse(
        Long id,
        Long listingId,
        String name,
        String roomNumber,
        Integer floorNumber,
        Integer capacity,
        BigDecimal monthlyRent,
        BigDecimal securityDeposit,
        RoomStatus status,
        String description,
        Long houseId,
        String houseName,
        String houseAddress,
        String housePostalCode,
        String housePrimaryImage,
        Long roomTypeId,
        String roomTypeName,
        String roomTypePrimaryImage,
        List<String> images,
        List<String> amenities,
        LocalDateTime publishedAt,
        PropertyType propertyType,
        ListingScope listingScope,
        Furnishing furnishing,
        LocalDate availableFrom,
        BigDecimal advancePayment,
        Integer bedrooms,
        Integer bathrooms,
        BigDecimal areaSqm,
        BigDecimal landSizeSqm,
        Integer numberOfFloors,
        String cityProvince,
        BigDecimal latitude,
        BigDecimal longitude,
        String googleMapsUrl,
        List<NearbyLocationResponse> nearbyLocations,
        List<PublicUtilityResponse> utilities,
        Long availableRoomCount,
        BigDecimal fromRent,
        List<AvailableRoomResponse> availableRooms,
        PublicOwnerResponse owner
) {
}
