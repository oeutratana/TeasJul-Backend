package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.AmenityRequest;
import com.example.spring_boot_project_api.dto.response.AmenityResponse;
import com.example.spring_boot_project_api.enums.AmenityStatus;
import com.example.spring_boot_project_api.model.Amenity;

@Component
public class AmenityMapper {
    public AmenityResponse toResponse(Amenity amenity) {
        return new AmenityResponse(
                amenity.getId(),
                amenity.getOwnerId(),
                amenity.getName(),
                amenity.getCode(),
                amenity.getDescription(),
                amenity.getIcon(),
                amenity.getStatus(),
                amenity.getCreatedAt(),
                amenity.getUpdatedAt());
    }

    public Amenity toEntity(AmenityRequest request) {
        return Amenity.builder()
                .name(request.name())
                .code(request.code())
                .description(request.description())
                .icon(request.icon())
                .status(request.status() != null ? request.status() : AmenityStatus.ACTIVE)
                .build();
    }
}
