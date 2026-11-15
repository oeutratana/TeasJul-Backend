package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.constraints.NotNull;

public record RoomUtilityRequest(
        @NotNull(message = "Room ID is required")
        Long roomId,

        @NotNull(message = "Utility type ID is required")
        Long utilityTypeId,

        Boolean isIncluded
) {
}
