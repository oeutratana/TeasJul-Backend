package com.example.spring_boot_project_api.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import com.example.spring_boot_project_api.enums.Furnishing;
import com.example.spring_boot_project_api.enums.ListingScope;
import com.example.spring_boot_project_api.enums.PropertyType;
import com.example.spring_boot_project_api.enums.PublicRoomSort;

/**
 * Filter and search criteria for public website room browsing.
 */
public record PublicRoomQueryRequest(
        String search,
        String cityProvince,
        PropertyType propertyType,
        Long roomTypeId,
        ListingScope listingScope,
        Furnishing furnishing,
        BigDecimal minRent,
        BigDecimal maxRent,
        Integer bedrooms,
        Integer bathrooms,
        BigDecimal minAreaSqm,
        BigDecimal maxAreaSqm,
        Integer capacity,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate availableFrom,
        List<String> amenities,
        List<String> utilities,
        PublicRoomSort sort
) {
}
