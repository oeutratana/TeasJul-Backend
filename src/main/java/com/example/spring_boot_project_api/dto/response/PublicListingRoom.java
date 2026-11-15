package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;

import com.example.spring_boot_project_api.enums.RoomStatus;

/**
 * Physical room exposed on a public grouped listing.
 */
public record PublicListingRoom(
        Long id,
        String roomNumber,
        String name,
        Integer floorNumber,
        Integer capacity,
        BigDecimal monthlyRent,
        BigDecimal securityDeposit,
        RoomStatus status
) {
}