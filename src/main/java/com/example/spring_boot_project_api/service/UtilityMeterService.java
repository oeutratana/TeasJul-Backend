package com.example.spring_boot_project_api.service;

import org.springframework.data.domain.Pageable;

import com.example.spring_boot_project_api.dto.request.CreateUtilityMeterRequest;
import com.example.spring_boot_project_api.dto.request.UpdateUtilityMeterRequest;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.UtilityMeterResponse;
import com.example.spring_boot_project_api.enums.UtilityMeterStatus;

public interface UtilityMeterService {

    UtilityMeterResponse createMeter(CreateUtilityMeterRequest request);

    PageResponse<UtilityMeterResponse> getMeters(String search, Long houseId, Long roomId,
            Long utilityTypeId, UtilityMeterStatus status, Pageable pageable);

    UtilityMeterResponse getMeterById(Long id);

    UtilityMeterResponse updateMeter(Long id, UpdateUtilityMeterRequest request);

    void deactivateMeter(Long id);
}
