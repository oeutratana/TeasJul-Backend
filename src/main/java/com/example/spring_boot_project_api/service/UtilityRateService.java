package com.example.spring_boot_project_api.service;

import java.time.LocalDate;

import org.springframework.data.domain.Pageable;

import com.example.spring_boot_project_api.dto.request.CreateUtilityRateRequest;
import com.example.spring_boot_project_api.dto.request.UpdateUtilityRateRequest;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.UtilityRateResponse;
import com.example.spring_boot_project_api.enums.UtilityRateStatus;

public interface UtilityRateService {

    UtilityRateResponse createRate(CreateUtilityRateRequest request);

    PageResponse<UtilityRateResponse> getRates(Long houseId, Long utilityTypeId,
            UtilityRateStatus status, LocalDate effectiveFrom, LocalDate effectiveTo, Pageable pageable);

    UtilityRateResponse getRateById(Long id);

    UtilityRateResponse updateRate(Long id, UpdateUtilityRateRequest request);

    void deactivateRate(Long id);
}
