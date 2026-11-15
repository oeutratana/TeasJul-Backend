package com.example.spring_boot_project_api.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.spring_boot_project_api.enums.Furnishing;
import com.example.spring_boot_project_api.enums.ListingScope;
import com.example.spring_boot_project_api.enums.PropertyType;
import com.example.spring_boot_project_api.enums.RoomStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoomRepuest {
    @NotNull(message = "House ID is required")
    private Long houseId;

    private Long roomTypeId;

    @Size(max = 50, message = "Room number must not exceed 50 characters")
    private String roomNumber;

    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    private Integer floorNumber;

    @NotNull(message = "Monthly rent is required")
    @PositiveOrZero(message = "Monthly rent cannot be negative")
    private BigDecimal monthlyRent = BigDecimal.ZERO;

    @NotNull(message = "Security deposit is required")
    @PositiveOrZero(message = "Security deposit cannot be negative")
    private BigDecimal securityDeposit = BigDecimal.ZERO;

    @Positive(message = "Capacity must be greater than 0")
    private Integer capacity = 1;

    private RoomStatus status = RoomStatus.AVAILABLE;

    private String description;

    private PropertyType propertyType;

    private ListingScope listingScope;

    private Furnishing furnishing;

    private LocalDate availableFrom;

    @PositiveOrZero(message = "Advance payment cannot be negative")
    private BigDecimal advancePayment;

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
}
