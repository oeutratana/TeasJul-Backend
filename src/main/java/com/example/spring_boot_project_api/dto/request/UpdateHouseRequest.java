package com.example.spring_boot_project_api.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.spring_boot_project_api.enums.Furnishing;
import com.example.spring_boot_project_api.enums.PropertyType;
import com.example.spring_boot_project_api.model.HouseStatus;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateHouseRequest(

        @Size(max = 200, message = "Name must not exceed 200 characters")
        String name,

        @Size(max = 200, message = "Slug must not exceed 200 characters")
        String slug,

        @Size(max = 50, message = "Code must not exceed 50 characters")
        String code,

        @Size(max = 500, message = "Address must not exceed 500 characters")
        String address,

        @Size(max = 20, message = "Postal code must not exceed 20 characters")
        String postalCode,

        BigDecimal latitude,

        BigDecimal longitude,

        @Size(max = 100, message = "City/Province must not exceed 100 characters")
        String cityProvince,

        @Size(max = 1000, message = "Google Maps URL must not exceed 1000 characters")
        String googleMapsUrl,

        Integer totalRooms,

        Integer availableRooms,

        String description,

        HouseStatus status,

        PropertyType propertyType,

        Furnishing furnishing,

        LocalDate availableFrom,

        @PositiveOrZero(message = "Advance payment cannot be negative")
        BigDecimal advancePayment,

        @Positive(message = "Bedrooms must be greater than 0")
        Integer bedrooms,

        @Positive(message = "Bathrooms must be greater than 0")
        Integer bathrooms,

        @Positive(message = "Area must be greater than 0")
        BigDecimal areaSqm,

        @Positive(message = "Land size must be greater than 0")
        BigDecimal landSizeSqm,

        @Positive(message = "Number of floors must be greater than 0")
        Integer numberOfFloors
) {
}
