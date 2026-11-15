package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDateTime;

public record RoomAmenityResponse(
        Long roomId,
        Long amenityId,
        String amenityName,
        String amenityCode,
        LocalDateTime createdAt
) {
}
