package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.spring_boot_project_api.enums.Furnishing;
import com.example.spring_boot_project_api.enums.ListingScope;
import com.example.spring_boot_project_api.enums.PropertyType;
import com.example.spring_boot_project_api.enums.RoomPublicationStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;

import lombok.Data;

@Data
public class RoomResponse {

    private Long id;
    private Long ownerId;
    private Long houseId;
    private Long roomTypeId;
    private String roomNumber;
    private String name;
    private String houseName;
    private String roomTypeName;
    private Integer floorNumber;
    private BigDecimal monthlyRent;
    private BigDecimal securityDeposit;
    private Integer capacity;
    private RoomStatus status;
    private String description;
    private PropertyType propertyType;
    private ListingScope listingScope;
    private Furnishing furnishing;
    private LocalDate availableFrom;
    private BigDecimal advancePayment;
    private Integer bedrooms;
    private Integer bathrooms;
    private BigDecimal areaSqm;
    private BigDecimal landSizeSqm;
    private Integer numberOfFloors;
    private Long listingId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private RoomTenantResponse tenant;

    private RoomPublicationStatus publicationStatus;
    private LocalDateTime submittedAt;
    private Long submittedBy;
    private LocalDateTime approvedAt;
    private Long approvedBy;
    private LocalDateTime publishedAt;
    private LocalDateTime rejectedAt;
    private Long rejectedBy;
    private String rejectionReason;
}
