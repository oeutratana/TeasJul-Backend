package com.example.spring_boot_project_api.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Shared amenity assigned at the listing level (mirrors {@link RoomAmenity}).
 */
@Entity
@jakarta.persistence.Table(name = "listing_amenities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingAmenity {

    @EmbeddedId
    private ListingAmenityId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("amenityId")
    @JoinColumn(name = "amenity_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_listing_amenities_amenity"))
    private Amenity amenity;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}