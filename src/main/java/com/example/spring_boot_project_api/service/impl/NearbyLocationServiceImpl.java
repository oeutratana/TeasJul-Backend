package com.example.spring_boot_project_api.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.CreateNearbyLocationRequest;
import com.example.spring_boot_project_api.dto.request.UpdateNearbyLocationRequest;
import com.example.spring_boot_project_api.dto.response.NearbyLocationResponse;
import com.example.spring_boot_project_api.exception.HouseNotFoundException;
import com.example.spring_boot_project_api.exception.NearbyLocationNotFoundException;
import com.example.spring_boot_project_api.mapper.NearbyLocationMapper;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.NearbyLocation;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.NearbyLocationRepository;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.service.NearbyLocationService;
import com.example.spring_boot_project_api.util.PermissionConstants;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NearbyLocationServiceImpl implements NearbyLocationService {

    private final NearbyLocationRepository nearbyLocationRepository;
    private final HouseRepository houseRepository;
    private final HouseAccessService houseAccessService;
    private final SecurityUtils securityUtils;
    private final NearbyLocationMapper nearbyLocationMapper;

    @Override
    @Transactional(readOnly = true)
    public List<NearbyLocationResponse> getNearbyLocations(Long houseId) {
        securityUtils.requirePermission(PermissionConstants.HOUSE_VIEW);
        houseAccessService.requireHouseAccess(houseId);
        return nearbyLocationRepository.findByHouseIdOrderByDistanceKmAscIdAsc(houseId)
                .stream()
                .map(nearbyLocationMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public NearbyLocationResponse getNearbyLocation(Long houseId, Long id) {
        securityUtils.requirePermission(PermissionConstants.HOUSE_VIEW);
        houseAccessService.requireHouseAccess(houseId);
        return nearbyLocationMapper.toResponse(requireOwnedNearbyLocation(houseId, id));
    }

    @Override
    @Transactional
    public NearbyLocationResponse createNearbyLocation(Long houseId, CreateNearbyLocationRequest request) {
        securityUtils.requirePermission(PermissionConstants.HOUSE_MANAGE);
        houseAccessService.requireHouseAccess(houseId);
        requireHouseExists(houseId);

        NearbyLocation entity = NearbyLocation.builder()
                .house(houseRepository.getReferenceById(houseId))
                .name(request.name())
                .category(request.category())
                .distanceKm(request.distanceKm())
                .travelMinutes(request.travelMinutes())
                .build();

        return nearbyLocationMapper.toResponse(nearbyLocationRepository.save(entity));
    }

    @Override
    @Transactional
    public NearbyLocationResponse updateNearbyLocation(Long houseId, Long id,
            UpdateNearbyLocationRequest request) {
        securityUtils.requirePermission(PermissionConstants.HOUSE_MANAGE);
        houseAccessService.requireHouseAccess(houseId);

        NearbyLocation entity = requireOwnedNearbyLocation(houseId, id);

        if (request.name() != null) {
            entity.setName(request.name());
        }
        if (request.category() != null) {
            entity.setCategory(request.category());
        }
        if (request.distanceKm() != null) {
            entity.setDistanceKm(request.distanceKm());
        }
        if (request.travelMinutes() != null) {
            entity.setTravelMinutes(request.travelMinutes());
        }

        return nearbyLocationMapper.toResponse(nearbyLocationRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteNearbyLocation(Long houseId, Long id) {
        securityUtils.requirePermission(PermissionConstants.HOUSE_MANAGE);
        houseAccessService.requireHouseAccess(houseId);

        NearbyLocation entity = requireOwnedNearbyLocation(houseId, id);
        nearbyLocationRepository.delete(entity);
    }

    private NearbyLocation requireOwnedNearbyLocation(Long houseId, Long id) {
        return nearbyLocationRepository.findByIdAndHouseId(id, houseId)
                .orElseThrow(() -> NearbyLocationNotFoundException.forId(id));
    }

    private void requireHouseExists(Long houseId) {
        if (!houseRepository.existsById(houseId)) {
            throw HouseNotFoundException.forId(houseId);
        }
    }
}