package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.CreateNearbyLocationRequest;
import com.example.spring_boot_project_api.dto.request.UpdateNearbyLocationRequest;
import com.example.spring_boot_project_api.dto.response.NearbyLocationResponse;

public interface NearbyLocationService {

    List<NearbyLocationResponse> getNearbyLocations(Long houseId);

    NearbyLocationResponse getNearbyLocation(Long houseId, Long id);

    NearbyLocationResponse createNearbyLocation(Long houseId, CreateNearbyLocationRequest request);

    NearbyLocationResponse updateNearbyLocation(Long houseId, Long id, UpdateNearbyLocationRequest request);

    void deleteNearbyLocation(Long houseId, Long id);
}