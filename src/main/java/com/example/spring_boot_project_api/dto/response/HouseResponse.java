package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.spring_boot_project_api.enums.Furnishing;
import com.example.spring_boot_project_api.enums.PropertyType;
import com.example.spring_boot_project_api.model.HouseStatus;

public record HouseResponse(
        Long id,
        Long ownerId,
        String name,
        String slug,
        String code,
        String address,
        String postalCode,
        BigDecimal latitude,
        BigDecimal longitude,
        String cityProvince,
        String googleMapsUrl,
        Integer totalRooms,
        Integer availableRooms,
        String description,
        HouseStatus status,
        PropertyType propertyType,
        Furnishing furnishing,
        LocalDate availableFrom,
        BigDecimal advancePayment,
        Integer bedrooms,
        Integer bathrooms,
        BigDecimal areaSqm,
        BigDecimal landSizeSqm,
        Integer numberOfFloors,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String primaryImage
) {

    /**
     * Returns a copy of this response with the room counts replaced by the
     * values derived from the actual {@code rooms} records of the house.
     * {@code totalRooms}/{@code availableRooms} are read-only, calculated
     * values and are never taken from user input.
     */
    public HouseResponse withRoomCounts(Integer totalRooms, Integer availableRooms) {
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
