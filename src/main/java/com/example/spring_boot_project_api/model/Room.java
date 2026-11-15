package com.example.spring_boot_project_api.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.example.spring_boot_project_api.enums.Furnishing;
import com.example.spring_boot_project_api.enums.ListingScope;
import com.example.spring_boot_project_api.enums.PropertyType;
import com.example.spring_boot_project_api.enums.RoomPublicationStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
@jakarta.persistence.Table(name = "rooms")
public class Room {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** TeasJul OWNER account owning this room, or null when the owner has no login. */
    @Column(name = "owner_id")
    private Long ownerId;

    /** Property ownership record when the owner does not have a TeasJul account. */
    @Column(name = "owner_record_id")
    private Long ownerRecordId;

    @Column(name = "house_id", nullable = false)
    private Long houseId;

    @Column(name = "room_type_id")
    private Long roomTypeId;

    @Column(name = "room_number", length = 50)
    private String roomNumber;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "floor_number")
    private Integer floorNumber;

    @Column(name = "monthly_rent", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyRent = BigDecimal.ZERO;

    @Column(name = "security_deposit", nullable = false, precision = 12, scale = 2)
    private BigDecimal securityDeposit = BigDecimal.ZERO;

    @Column(name = "capacity", nullable = false)
    private Integer capacity = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "ENUM('AVAILABLE','BOOKED','OCCUPIED','MAINTENANCE','RESERVED') DEFAULT 'AVAILABLE'")
    private RoomStatus status = RoomStatus.AVAILABLE;

    @Enumerated(EnumType.STRING)
    @Column(name = "publication_status", nullable = false, columnDefinition = "ENUM('DRAFT','PENDING_APPROVAL','PUBLISHED','REJECTED','UNPUBLISHED') DEFAULT 'DRAFT'")
    private RoomPublicationStatus publicationStatus = RoomPublicationStatus.DRAFT;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "submitted_by")
    private Long submittedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejected_by")
    private Long rejectedBy;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_type")
    private PropertyType propertyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "listing_scope")
    private ListingScope listingScope;

    @Enumerated(EnumType.STRING)
    @Column(name = "furnishing")
    private Furnishing furnishing;

    @Column(name = "available_from")
    private LocalDate availableFrom;

    @Column(name = "advance_payment", precision = 12, scale = 2)
    private BigDecimal advancePayment;

    @Column(name = "bedrooms")
    private Integer bedrooms;

    @Column(name = "bathrooms")
    private Integer bathrooms;

    @Column(name = "area_sqm", precision = 10, scale = 2)
    private BigDecimal areaSqm;

    @Column(name = "land_size_sqm", precision = 10, scale = 2)
    private BigDecimal landSizeSqm;

    @Column(name = "number_of_floors")
    private Integer numberOfFloors;

    @Column(name = "listing_id")
    private Long listingId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
