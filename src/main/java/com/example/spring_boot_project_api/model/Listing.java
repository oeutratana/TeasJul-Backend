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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A grouped multi-room listing: a public presentation that represents MULTIPLE
 * physical rooms of the SAME {@link RoomType} under one {@link House}.
 *
 * <p>One row per {@code (house_id, room_type_id)} pair. The physical rooms of
 * the group are linked through {@code rooms.listing_id}. Amenities, utilities,
 * images and the blueprint are configured here once and shared by the group —
 * never duplicated onto each room.</p>
 */
@Entity
@Table(
    name = "listings",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_listings_house_room_type",
        columnNames = {"house_id", "room_type_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "house_id", nullable = false)
    private Long houseId;

    @Column(name = "room_type_id", nullable = false)
    private Long roomTypeId;

    @Column(name = "title", length = 120)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_type")
    private PropertyType propertyType;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "listing_scope", nullable = false, length = 30)
    private ListingScope listingScope = ListingScope.MULTIPLE_ROOMS;

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

    @Column(name = "security_deposit", precision = 12, scale = 2)
    private BigDecimal securityDeposit;

    @Column(name = "blueprint_public_id", length = 255)
    private String blueprintPublicId;

    @Column(name = "blueprint_secure_url", length = 1024)
    private String blueprintSecureUrl;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "publication_status", nullable = false, length = 30)
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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}