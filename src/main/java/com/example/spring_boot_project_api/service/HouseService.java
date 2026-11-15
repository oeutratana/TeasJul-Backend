package com.example.spring_boot_project_api.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.example.spring_boot_project_api.dto.request.CreateHouseRequest;
import com.example.spring_boot_project_api.dto.request.UpdateHouseRequest;
import com.example.spring_boot_project_api.dto.response.HouseResponse;
import com.example.spring_boot_project_api.dto.response.HouseSummaryResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.RentalContractResponse;
import com.example.spring_boot_project_api.dto.response.RoomResponse;
import com.example.spring_boot_project_api.dto.response.TenantResponse;
import com.example.spring_boot_project_api.enums.RentalContractStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;

public interface HouseService {

    HouseResponse createHouse(CreateHouseRequest request);

    HouseResponse getHouseById(Long id);

    List<HouseResponse> getMyHouses();

    HouseResponse updateHouse(Long id, UpdateHouseRequest request);

    void deleteHouse(Long id);

    HouseSummaryResponse getHouseSummary(Long houseId);

    PageResponse<RoomResponse> getHouseRooms(Long houseId, String search, RoomStatus status,
            Long roomTypeId, Pageable pageable);

    PageResponse<TenantResponse> getHouseTenants(Long houseId, String search, Pageable pageable);

    PageResponse<RentalContractResponse> getHouseContracts(Long houseId, String search,
            RentalContractStatus status, Pageable pageable);
}