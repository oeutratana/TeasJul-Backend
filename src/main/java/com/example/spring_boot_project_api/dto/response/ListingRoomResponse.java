package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.spring_boot_project_api.enums.RoomPublicationStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;

/**
 * Physical room attached to a grouped listing.
 */
public record ListingRoomResponse(
        Long id,
        String roomNumber,
        String name,
        Integer floorNumber,
        BigDecimal monthlyRent,
        BigDecimal securityDeposit,
        RoomStatus status,
        RoomPublicationStatus publicationStatus,
        LocalDateTime createdAt
) {
}