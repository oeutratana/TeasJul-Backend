package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.RoomAmenityResponse;
import com.example.spring_boot_project_api.model.Amenity;
import com.example.spring_boot_project_api.model.RoomAmenity;
import com.example.spring_boot_project_api.model.RoomAmenityId;


@Component
public class RoomAmenityMapper {

    public RoomAmenity toEntity(Long roomId, Amenity amenity) {
        return RoomAmenity.builder()
                .id(new RoomAmenityId(roomId, amenity.getId()))
                .amenity(amenity)
                .build();
    }

    public RoomAmenityResponse toResponse(RoomAmenity roomAmenity) {
        Amenity amenity = roomAmenity.getAmenity();
        return new RoomAmenityResponse(
                roomAmenity.getId().getRoomId(),
                amenity.getId(),
                amenity.getName(),
                amenity.getCode(),
                roomAmenity.getCreatedAt());
    }
}
