package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.AmenityRequest;
import com.example.spring_boot_project_api.dto.request.UpdateAmenityRequest;
import com.example.spring_boot_project_api.dto.response.AmenityResponse;
import com.example.spring_boot_project_api.enums.AmenityStatus;

public interface AmenityService {

    AmenityResponse createAmenity(AmenityRequest request);

    AmenityResponse getAmenityById(Long id);

    List<AmenityResponse> getMyAmenities();

    List<AmenityResponse> getMyAmenitiesByStatus(AmenityStatus status);

    AmenityResponse updateAmenity(Long id, UpdateAmenityRequest request);

    void deleteAmenity(Long id);
}
