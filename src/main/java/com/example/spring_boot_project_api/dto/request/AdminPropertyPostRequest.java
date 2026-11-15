package com.example.spring_boot_project_api.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.example.spring_boot_project_api.enums.Furnishing;
import com.example.spring_boot_project_api.enums.PropertyType;
import com.example.spring_boot_project_api.enums.RoomStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * One-shot payload for the ADMIN/SUPER_ADMIN property posting flow.
 *
 * <p>The owner is specified by EXACTLY ONE of {@code ownerUserId} (an existing
 * TeasJul OWNER account), {@code ownerRecordId}, or an inline {@code ownerRecord}
 * (a non-login property owner). No login account is ever auto-created.</p>
 *
 * <p>The property type drives validation: HOUSE/VILLA behave as whole-property
 * listings, APARTMENT/CONDO as units with an optional floor, and ROOM keeps the
 * full room model (house + room type + room number + capacity).</p>
 */
@Data
public class AdminPropertyPostRequest {

    @NotNull(message = "Property type is required")
    private PropertyType propertyType;

    private Long ownerUserId;
    private Long ownerRecordId;
    @Valid
    private PropertyOwnerRequest ownerRecord;

    private Long houseId;
    @Valid
    private AdminPropertyHouseRequest house;

    private Long roomTypeId;
    @Valid
    private AdminPropertyRoomTypeRequest roomType;

    @Size(max = 50, message = "Room/unit number must not exceed 50 characters")
    private String roomNumber;

    @Size(max = 100, message = "Room name must not exceed 100 characters")
    private String name;

    private Integer floorNumber;

    @Positive(message = "Capacity must be greater than 0")
    private Integer capacity;

    @NotNull(message = "Monthly rent is required")
    @PositiveOrZero(message = "Monthly rent cannot be negative")
    private BigDecimal monthlyRent = BigDecimal.ZERO;

    @NotNull(message = "Security deposit is required")
    @PositiveOrZero(message = "Security deposit cannot be negative")
    private BigDecimal securityDeposit = BigDecimal.ZERO;

    @PositiveOrZero(message = "Advance payment cannot be negative")
    private BigDecimal advancePayment;

    private LocalDate availableFrom;

    private RoomStatus status;

    private Furnishing furnishing;

    private String description;

    @Positive(message = "Bedrooms must be greater than 0")
    private Integer bedrooms;

    @Positive(message = "Bathrooms must be greater than 0")
    private Integer bathrooms;

    @Positive(message = "Area must be greater than 0")
    private BigDecimal areaSqm;

    @Positive(message = "Land size must be greater than 0")
    private BigDecimal landSizeSqm;

    @Positive(message = "Number of floors must be greater than 0")
    private Integer numberOfFloors;

    @Valid
    private List<Long> amenityIds = List.of();

    @Valid
    private List<AdminPropertyUtilityRequest> utilities = List.of();

    @Valid
    private List<CreateNearbyLocationRequest> nearbyLocations = List.of();

    /**
     * When true the post is created (or moved) straight to PUBLISHED and
     * approved by the issuing SUPER_ADMIN, mirroring the existing
     * {@code publishImmediately} admin rule. When false the post is a DRAFT.
     */
    private boolean publish;
}