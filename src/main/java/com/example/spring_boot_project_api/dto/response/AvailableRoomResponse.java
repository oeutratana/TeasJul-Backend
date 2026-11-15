package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;

import com.example.spring_boot_project_api.enums.RoomStatus;

/**
 * Public-safe summary of one physical room available within a room-type group.
 */
public record AvailableRoomResponse(
        Long id,
        String roomNumber,
        Integer floorNumber,
        BigDecimal monthlyRent,
        BigDecimal securityDeposit,
        RoomStatus status
) {
}
