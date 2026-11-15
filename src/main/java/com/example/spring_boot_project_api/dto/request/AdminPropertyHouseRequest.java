package com.example.spring_boot_project_api.dto.request;

import java.math.BigDecimal;

import com.example.spring_boot_project_api.enums.Furnishing;
import com.example.spring_boot_project_api.enums.PropertyType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Building / property information for an admin-posted property. Used to create
 * the backing {@code houses} row when the property does not reuse an existing
 * house. Required for HOUSE/VILLA/APARTMENT/CONDO posts and optional (to add a
 * new building) for ROOM posts.
 */
@Data
public class AdminPropertyHouseRequest {

    @NotBlank(message = "Property name is required")
    @Size(max = 200, message = "Property name must not exceed 200 characters")
    private String name;

    @NotBlank(message = "Address is required")
    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    @Size(max = 100, message = "City/Province must not exceed 100 characters")
    private String cityProvince;

    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    private String postalCode;

    private String description;

    private PropertyType propertyType;

    private Furnishing furnishing;

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