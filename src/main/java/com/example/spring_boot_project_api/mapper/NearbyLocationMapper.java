package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.NearbyLocationResponse;
import com.example.spring_boot_project_api.model.NearbyLocation;

@Component
public class NearbyLocationMapper {

    public NearbyLocationResponse toResponse(NearbyLocation nearbyLocation) {
        return new NearbyLocationResponse(
                nearbyLocation.getId(),
                nearbyLocation.getHouse().getId(),
                nearbyLocation.getName(),
                nearbyLocation.getCategory(),
                nearbyLocation.getDistanceKm(),
                nearbyLocation.getTravelMinutes());
    }
}