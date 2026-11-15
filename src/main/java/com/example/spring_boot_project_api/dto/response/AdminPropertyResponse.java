package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.example.spring_boot_project_api.enums.Furnishing;
import com.example.spring_boot_project_api.enums.ListingScope;
import com.example.spring_boot_project_api.enums.PropertyType;
import com.example.spring_boot_project_api.enums.RoomPublicationStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;

import lombok.Data;

/**
 * Full admin property-post view: the posted {@code rooms} listing plus its
 * backing house, owner summary and attached amenities/utilities/nearby
 * locations. Used by the admin property listing, drafts and continue-editing
 * flows.
 */
@Data
public class AdminPropertyResponse {

    private Long roomId;
    private Long houseId;
    private String propertyName;
    private String address;
    private String postalCode;
    private String cityProvince;
    private PropertyType propertyType;
    private ListingScope listingScope;
    private RoomPublicationStatus publicationStatus;

    private Long ownerUserId;
    private Long propertyOwnerId;
    private String ownerName;
    private String ownerEmail;
    private String ownerPhone;
    private boolean accountOwner;

    private String roomNumber;
    private String roomName;
    private Integer floorNumber;
    private Integer capacity;
    private Long roomTypeId;
    private String roomTypeName;

    private BigDecimal monthlyRent;
    private BigDecimal securityDeposit;
    private BigDecimal advancePayment;
    private LocalDate availableFrom;
    private RoomStatus status;
    private Furnishing furnishing;
    private String description;
    private Integer bedrooms;
    private Integer bathrooms;
    private BigDecimal areaSqm;
    private BigDecimal landSizeSqm;
    private Integer numberOfFloors;

    private List<Long> amenityIds;
    private List<String> amenityNames;
    private List<AdminPropertyUtilityResponse> utilities;
    private List<NearbyLocationResponse> nearbyLocations;
    private List<String> photoUrls;

    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
}