package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.CreateUtilityMeterRequest;
import com.example.spring_boot_project_api.dto.response.UtilityMeterResponse;
import com.example.spring_boot_project_api.enums.UtilityMeterStatus;
import com.example.spring_boot_project_api.model.UtilityMeter;

@Component
public class UtilityMeterMapper {

    public UtilityMeterResponse toResponse(UtilityMeter meter) {
        return new UtilityMeterResponse(
                meter.getId(),
                meter.getOwner().getId(),
                meter.getHouseId(),
                meter.getRoomId(),
                meter.getUtilityTypeId(),
                meter.getMeterNumber(),
                meter.getInstalledAt(),
                meter.getStatus(),
                meter.getCreatedAt(),
                meter.getUpdatedAt());
    }

    public UtilityMeter toEntity(CreateUtilityMeterRequest request) {
        return UtilityMeter.builder()
                .houseId(request.houseId())
                .roomId(request.roomId())
                .utilityTypeId(request.utilityTypeId())
                .meterNumber(request.meterNumber())
                .installedAt(request.installedAt())
                .status(request.status() != null ? request.status() : UtilityMeterStatus.ACTIVE)
                .build();
    }
}
