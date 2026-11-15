package com.example.spring_boot_project_api.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.spring_boot_project_api.dto.response.HouseResponse;
import com.example.spring_boot_project_api.enums.Furnishing;
import com.example.spring_boot_project_api.enums.PropertyType;
import com.example.spring_boot_project_api.model.HouseStatus;

/**
 * Reusable test helper for building {@link HouseResponse} instances without
 * depending on the record's positional constructor order. New HouseResponse
 * fields only require a new {@code withXxx} method here instead of breaking
 * every test that constructs one.
 */
public final class HouseResponseTestBuilder {

    private Long id;
    private Long ownerId;
    private String name;
    private String slug;
    private String code;
    private String address;
    private String postalCode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer totalRooms;
    private Integer availableRooms;
    private String description;
    private HouseStatus status;
    private PropertyType propertyType;
    private Furnishing furnishing;
    private LocalDate availableFrom;
    private BigDecimal advancePayment;
    private Integer bedrooms;
    private Integer bathrooms;
    private BigDecimal areaSqm;
    private BigDecimal landSizeSqm;
    private Integer numberOfFloors;
    private String cityProvince;
    private String googleMapsUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String primaryImage;

    public static HouseResponseTestBuilder houseResponse() {
        return new HouseResponseTestBuilder();
    }

    public HouseResponseTestBuilder id(Long id) {
        this.id = id;
        return this;
    }

    public HouseResponseTestBuilder ownerId(Long ownerId) {
        this.ownerId = ownerId;
        return this;
    }

    public HouseResponseTestBuilder name(String name) {
        this.name = name;
        return this;
    }

    public HouseResponseTestBuilder slug(String slug) {
        this.slug = slug;
        return this;
    }

    public HouseResponseTestBuilder code(String code) {
        this.code = code;
        return this;
    }

    public HouseResponseTestBuilder address(String address) {
        this.address = address;
        return this;
    }

    public HouseResponseTestBuilder postalCode(String postalCode) {
        this.postalCode = postalCode;
        return this;
    }

    public HouseResponseTestBuilder latitude(BigDecimal latitude) {
        this.latitude = latitude;
        return this;
    }

    public HouseResponseTestBuilder longitude(BigDecimal longitude) {
        this.longitude = longitude;
        return this;
    }

    public HouseResponseTestBuilder cityProvince(String cityProvince) {
        this.cityProvince = cityProvince;
        return this;
    }

    public HouseResponseTestBuilder googleMapsUrl(String googleMapsUrl) {
        this.googleMapsUrl = googleMapsUrl;
        return this;
    }

    public HouseResponseTestBuilder totalRooms(Integer totalRooms) {
        this.totalRooms = totalRooms;
        return this;
    }

    public HouseResponseTestBuilder availableRooms(Integer availableRooms) {
        this.availableRooms = availableRooms;
        return this;
    }

    public HouseResponseTestBuilder description(String description) {
        this.description = description;
        return this;
    }

    public HouseResponseTestBuilder status(HouseStatus status) {
        this.status = status;
        return this;
    }

    public HouseResponseTestBuilder propertyType(PropertyType propertyType) {
        this.propertyType = propertyType;
        return this;
    }

    public HouseResponseTestBuilder furnishing(Furnishing furnishing) {
        this.furnishing = furnishing;
        return this;
    }

    public HouseResponseTestBuilder availableFrom(LocalDate availableFrom) {
        this.availableFrom = availableFrom;
        return this;
    }

    public HouseResponseTestBuilder advancePayment(BigDecimal advancePayment) {
        this.advancePayment = advancePayment;
        return this;
    }

    public HouseResponseTestBuilder bedrooms(Integer bedrooms) {
        this.bedrooms = bedrooms;
        return this;
    }

    public HouseResponseTestBuilder bathrooms(Integer bathrooms) {
        this.bathrooms = bathrooms;
        return this;
    }

    public HouseResponseTestBuilder areaSqm(BigDecimal areaSqm) {
        this.areaSqm = areaSqm;
        return this;
    }

    public HouseResponseTestBuilder landSizeSqm(BigDecimal landSizeSqm) {
        this.landSizeSqm = landSizeSqm;
        return this;
    }

    public HouseResponseTestBuilder numberOfFloors(Integer numberOfFloors) {
        this.numberOfFloors = numberOfFloors;
        return this;
    }

    public HouseResponseTestBuilder createdAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public HouseResponseTestBuilder updatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public HouseResponseTestBuilder primaryImage(String primaryImage) {
        this.primaryImage = primaryImage;
        return this;
    }

    public HouseResponse build() {
        return new HouseResponse(
                id,
                ownerId,
                name,
                slug,
                code,
                address,
                postalCode,
                latitude,
                longitude,
                cityProvince,
                googleMapsUrl,
                totalRooms,
                availableRooms,
                description,
                status,
                propertyType,
                furnishing,
                availableFrom,
                advancePayment,
                bedrooms,
                bathrooms,
                areaSqm,
                landSizeSqm,
                numberOfFloors,
                createdAt,
                updatedAt,
                primaryImage);
    }
}