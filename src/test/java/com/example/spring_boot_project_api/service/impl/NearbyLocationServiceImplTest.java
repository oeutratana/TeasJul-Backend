package com.example.spring_boot_project_api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import com.example.spring_boot_project_api.dto.request.CreateNearbyLocationRequest;
import com.example.spring_boot_project_api.dto.request.UpdateNearbyLocationRequest;
import com.example.spring_boot_project_api.dto.response.NearbyLocationResponse;
import com.example.spring_boot_project_api.enums.NearbyLocationCategory;
import com.example.spring_boot_project_api.exception.HouseNotFoundException;
import com.example.spring_boot_project_api.exception.NearbyLocationNotFoundException;
import com.example.spring_boot_project_api.mapper.NearbyLocationMapper;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.NearbyLocation;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.NearbyLocationRepository;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NearbyLocationServiceImplTest {

    private static final long HOUSE_ID = 7L;
    private static final long LOCATION_ID = 100L;

    private NearbyLocationRepository nearbyLocationRepository;
    private HouseRepository houseRepository;
    private HouseAccessService houseAccessService;
    private SecurityUtils securityUtils;
    private NearbyLocationServiceImpl service;

    @BeforeEach
    void setUp() {
        nearbyLocationRepository = mock(NearbyLocationRepository.class);
        houseRepository = mock(HouseRepository.class);
        houseAccessService = mock(HouseAccessService.class);
        securityUtils = mock(SecurityUtils.class);
        service = new NearbyLocationServiceImpl(
                nearbyLocationRepository,
                houseRepository,
                houseAccessService,
                securityUtils,
                new NearbyLocationMapper());

        when(nearbyLocationRepository.save(any(NearbyLocation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private House house() {
        User owner = new User();
        owner.setId(42L);
        return House.builder().id(HOUSE_ID).owner(owner).name("Villa Mondul").build();
    }

    private NearbyLocation location() {
        return NearbyLocation.builder()
                .id(LOCATION_ID)
                .house(house())
                .name("Royal University")
                .category(NearbyLocationCategory.UNIVERSITY)
                .distanceKm(new BigDecimal("1.50"))
                .travelMinutes(12)
                .build();
    }

    private CreateNearbyLocationRequest createRequest() {
        return new CreateNearbyLocationRequest(
                "Central Market", NearbyLocationCategory.MARKET,
                new BigDecimal("0.80"), 6);
    }

    private UpdateNearbyLocationRequest updateRequest() {
        return new UpdateNearbyLocationRequest(
                "Boeung Keng Kang Market", NearbyLocationCategory.MARKET,
                new BigDecimal("2.10"), 15);
    }

    @Test
    void getNearbyLocationsReturnsAllForAccessibleHouse() {
        when(nearbyLocationRepository.findByHouseIdOrderByDistanceKmAscIdAsc(HOUSE_ID))
                .thenReturn(List.of(location()));

        List<NearbyLocationResponse> result = service.getNearbyLocations(HOUSE_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(LOCATION_ID);
        assertThat(result.get(0).name()).isEqualTo("Royal University");
        assertThat(result.get(0).category()).isEqualTo(NearbyLocationCategory.UNIVERSITY);
        assertThat(result.get(0).distanceKm()).isEqualByComparingTo(new BigDecimal("1.50"));
        assertThat(result.get(0).travelMinutes()).isEqualTo(12);
        assertThat(result.get(0).houseId()).isEqualTo(HOUSE_ID);
    }

    @Test
    void getNearbyLocationsDeniesWhenNoHouseAccess() {
        doThrow(new AccessDeniedException("denied"))
                .when(houseAccessService).requireHouseAccess(HOUSE_ID);

        assertThatThrownBy(() -> service.getNearbyLocations(HOUSE_ID))
                .isInstanceOf(AccessDeniedException.class);

        verify(nearbyLocationRepository, never()).findByHouseIdOrderByDistanceKmAscIdAsc(anyLong());
    }

    @Test
    void getNearbyLocationReturnsOwnedLocation() {
        when(nearbyLocationRepository.findByIdAndHouseId(LOCATION_ID, HOUSE_ID))
                .thenReturn(Optional.of(location()));

        NearbyLocationResponse result = service.getNearbyLocation(HOUSE_ID, LOCATION_ID);

        assertThat(result.id()).isEqualTo(LOCATION_ID);
        assertThat(result.name()).isEqualTo("Royal University");
    }

    @Test
    void getNearbyLocationThrowsNotFoundWhenLocationDoesNotBelongToHouse() {
        when(nearbyLocationRepository.findByIdAndHouseId(LOCATION_ID, HOUSE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getNearbyLocation(HOUSE_ID, LOCATION_ID))
                .isInstanceOf(NearbyLocationNotFoundException.class);
    }

    @Test
    void createNearbyLocationPersistsEntityForOwnedHouse() {
        when(houseRepository.existsById(HOUSE_ID)).thenReturn(true);
        when(houseRepository.getReferenceById(HOUSE_ID)).thenReturn(house());

        NearbyLocationResponse result = service.createNearbyLocation(HOUSE_ID, createRequest());

        assertThat(result.name()).isEqualTo("Central Market");
        assertThat(result.category()).isEqualTo(NearbyLocationCategory.MARKET);
        assertThat(result.distanceKm()).isEqualByComparingTo(new BigDecimal("0.80"));
        assertThat(result.travelMinutes()).isEqualTo(6);

        verify(nearbyLocationRepository).save(any(NearbyLocation.class));
    }

    @Test
    void createNearbyLocationThrowsWhenHouseDoesNotExist() {
        when(houseRepository.existsById(HOUSE_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.createNearbyLocation(HOUSE_ID, createRequest()))
                .isInstanceOf(HouseNotFoundException.class);
    }

    @Test
    void updateNearbyLocationAppliesNonNullFields() {
        when(nearbyLocationRepository.findByIdAndHouseId(LOCATION_ID, HOUSE_ID))
                .thenReturn(Optional.of(location()));

        NearbyLocationResponse result = service.updateNearbyLocation(
                HOUSE_ID, LOCATION_ID, updateRequest());

        assertThat(result.name()).isEqualTo("Boeung Keng Kang Market");
        assertThat(result.category()).isEqualTo(NearbyLocationCategory.MARKET);
        assertThat(result.distanceKm()).isEqualByComparingTo(new BigDecimal("2.10"));
        assertThat(result.travelMinutes()).isEqualTo(15);
    }

    @Test
    void updateNearbyLocationWithEmptyRequestKeepsExistingValues() {
        when(nearbyLocationRepository.findByIdAndHouseId(LOCATION_ID, HOUSE_ID))
                .thenReturn(Optional.of(location()));

        NearbyLocationResponse result = service.updateNearbyLocation(
                HOUSE_ID, LOCATION_ID, new UpdateNearbyLocationRequest(null, null, null, null));

        assertThat(result.name()).isEqualTo("Royal University");
        assertThat(result.category()).isEqualTo(NearbyLocationCategory.UNIVERSITY);
        assertThat(result.distanceKm()).isEqualByComparingTo(new BigDecimal("1.50"));
        assertThat(result.travelMinutes()).isEqualTo(12);
    }

    @Test
    void updateNearbyLocationThrowsNotFoundWhenLocationDoesNotBelongToHouse() {
        when(nearbyLocationRepository.findByIdAndHouseId(LOCATION_ID, HOUSE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateNearbyLocation(
                HOUSE_ID, LOCATION_ID, updateRequest()))
                .isInstanceOf(NearbyLocationNotFoundException.class);
    }

    @Test
    void deleteNearbyLocationRemovesOwnedLocation() {
        NearbyLocation existing = location();
        when(nearbyLocationRepository.findByIdAndHouseId(LOCATION_ID, HOUSE_ID))
                .thenReturn(Optional.of(existing));

        service.deleteNearbyLocation(HOUSE_ID, LOCATION_ID);

        verify(nearbyLocationRepository).delete(existing);
    }

    @Test
    void deleteNearbyLocationThrowsNotFoundWhenLocationDoesNotBelongToHouse() {
        when(nearbyLocationRepository.findByIdAndHouseId(LOCATION_ID, HOUSE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteNearbyLocation(HOUSE_ID, LOCATION_ID))
                .isInstanceOf(NearbyLocationNotFoundException.class);

        verify(nearbyLocationRepository, never()).delete(any(NearbyLocation.class));
    }

    @Test
    void writeOperationsAreDeniedWithoutHouseManageAccess() {
        doThrow(new AccessDeniedException("denied"))
                .when(houseAccessService).requireHouseAccess(HOUSE_ID);

        assertThatThrownBy(() -> service.createNearbyLocation(HOUSE_ID, createRequest()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.updateNearbyLocation(HOUSE_ID, LOCATION_ID, updateRequest()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.deleteNearbyLocation(HOUSE_ID, LOCATION_ID))
                .isInstanceOf(AccessDeniedException.class);

        verify(nearbyLocationRepository, never()).save(any(NearbyLocation.class));
        verify(nearbyLocationRepository, never()).delete(any(NearbyLocation.class));
    }
}