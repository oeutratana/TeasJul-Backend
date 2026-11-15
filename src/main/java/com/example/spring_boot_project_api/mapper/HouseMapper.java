package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.CreateHouseRequest;
import com.example.spring_boot_project_api.dto.response.HouseResponse;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.HouseStatus;

@Component
public class HouseMapper {

    public HouseResponse toResponse(House house) {
        return toResponse(house, null);
    }

    public HouseResponse toResponse(House house, String primaryImage) {
        return new HouseResponse(
                house.getId(),
                house.getOwner().getId(),
                house.getName(),
                house.getSlug(),
                house.getCode(),
                house.getAddress(),
                house.getPostalCode(),
                house.getLatitude(),
                house.getLongitude(),
                house.getCityProvince(),
                house.getGoogleMapsUrl(),
                house.getTotalRooms(),
                house.getAvailableRooms(),
                house.getDescription(),
                house.getStatus(),
                house.getPropertyType(),
                house.getFurnishing(),
                house.getAvailableFrom(),
                house.getAdvancePayment(),
                house.getBedrooms(),
                house.getBathrooms(),
                house.getAreaSqm(),
                house.getLandSizeSqm(),
                house.getNumberOfFloors(),
                house.getCreatedAt(),
                house.getUpdatedAt(),
                primaryImage);
    }

    public House toEntity(CreateHouseRequest request) {
        return House.builder()
                .name(request.name())
                .slug(request.slug())
                .code(request.code())
                .address(request.address())
                .postalCode(request.postalCode())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .cityProvince(request.cityProvince())
                .googleMapsUrl(request.googleMapsUrl())
                .totalRooms(request.totalRooms() != null ? request.totalRooms() : 0)
                .availableRooms(request.availableRooms() != null ? request.availableRooms() : 0)
                .description(request.description())
                .status(HouseStatus.ACTIVE)
                .propertyType(request.propertyType())
                .furnishing(request.furnishing())
                .availableFrom(request.availableFrom())
                .advancePayment(request.advancePayment())
                .bedrooms(request.bedrooms())
                .bathrooms(request.bathrooms())
                .areaSqm(request.areaSqm())
                .landSizeSqm(request.landSizeSqm())
                .numberOfFloors(request.numberOfFloors())
                .build();
    }
}
