package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.RoomUtilityResponse;
import com.example.spring_boot_project_api.model.RoomUtility;
import com.example.spring_boot_project_api.model.UtilityType;

@Component
public class RoomUtilityMapper {

    public RoomUtilityResponse toResponse(RoomUtility ru, UtilityType type, String roomNumber) {
        return new RoomUtilityResponse(
                ru.getId(),
                ru.getRoomId(),
                roomNumber,
                ru.getUtilityTypeId(),
                type.getName(),
                type.getCode(),
                type.getBillingMethod(),
                ru.getStatus(),
                ru.getCreatedAt(),
                Boolean.TRUE.equals(ru.getIsIncluded()));
    }
}
