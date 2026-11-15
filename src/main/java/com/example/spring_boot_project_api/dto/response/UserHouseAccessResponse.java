package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDateTime;

public record UserHouseAccessResponse(
        Long userId,
        String username,
        Long houseId,
        LocalDateTime createdAt
) {
}
