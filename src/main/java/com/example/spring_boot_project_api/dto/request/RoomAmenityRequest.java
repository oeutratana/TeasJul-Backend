package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.constraints.NotNull;

public record RoomAmenityRequest(

    @NotNull(message = "Room ID is required")
    Long roomId,

    @NotNull(message = "Amenity ID is required")
    Long amenityId

){}
