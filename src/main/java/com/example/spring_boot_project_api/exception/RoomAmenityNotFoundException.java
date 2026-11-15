package com.example.spring_boot_project_api.exception;

public class RoomAmenityNotFoundException extends RuntimeException {

    public RoomAmenityNotFoundException(String message) {
        super(message);
    }

    public static RoomAmenityNotFoundException forIds(Long roomId, Long amenityId) {
        return new RoomAmenityNotFoundException(
                "Amenity " + amenityId + " is not assigned to room " + roomId);
    }
}
