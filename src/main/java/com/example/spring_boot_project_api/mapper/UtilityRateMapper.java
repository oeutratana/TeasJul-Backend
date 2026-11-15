package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.CreateUtilityRateRequest;
import com.example.spring_boot_project_api.dto.response.UtilityRateResponse;
import com.example.spring_boot_project_api.enums.UtilityRateStatus;
import com.example.spring_boot_project_api.model.UtilityRate;

@Component
public class UtilityRateMapper {

    public UtilityRateResponse toResponse(UtilityRate rate) {
        return new UtilityRateResponse(
                rate.getId(),
                rate.getOwner().getId(),
                rate.getHouseId(),
                rate.getUtilityTypeId(),
                rate.getRate(),
                rate.getEffectiveFrom(),
                rate.getEffectiveTo(),
                rate.getStatus(),
                rate.getCreatedAt(),
                rate.getUpdatedAt());
    }

    public UtilityRate toEntity(CreateUtilityRateRequest request) {
        return UtilityRate.builder()
                .houseId(request.houseId())
                .utilityTypeId(request.utilityTypeId())
                .rate(request.rate())
                .effectiveFrom(request.effectiveFrom())
                .effectiveTo(request.effectiveTo())
                .status(request.status() != null ? request.status() : UtilityRateStatus.ACTIVE)
                .build();
    }
}
