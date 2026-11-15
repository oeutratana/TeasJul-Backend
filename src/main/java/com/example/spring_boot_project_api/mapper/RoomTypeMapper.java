package com.example.spring_boot_project_api.mapper;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.RoomTypeRequest;
import com.example.spring_boot_project_api.dto.response.RoomTypeResponse;
import com.example.spring_boot_project_api.enums.RoomTypeStatus;
import com.example.spring_boot_project_api.model.RoomType;

@Component
public class RoomTypeMapper {

    public RoomType toEntity(RoomTypeRequest dto) {
        if (dto == null) return null;

        RoomType roomType = new RoomType();
        roomType.setName(dto.getName());
        roomType.setCode(dto.getCode());
        roomType.setDescription(dto.getDescription());
        roomType.setCapacity(dto.getCapacity() != null ? dto.getCapacity() : 1);
        roomType.setDefaultRent(dto.getDefaultRent() != null ? dto.getDefaultRent() : BigDecimal.ZERO);
        roomType.setDefaultDeposit(dto.getDefaultDeposit() != null ? dto.getDefaultDeposit() : BigDecimal.ZERO);
        roomType.setStatus(dto.getStatus() != null ? dto.getStatus() : RoomTypeStatus.ACTIVE);
        return roomType;
    }

    public RoomTypeResponse toDto(RoomType entity) {
        return toDto(entity, null);
    }

    public RoomTypeResponse toDto(RoomType entity, String primaryImage) {
        if (entity == null) return null;

        RoomTypeResponse response = new RoomTypeResponse();
        response.setId(entity.getId());
        response.setOwnerId(entity.getOwnerId());
        response.setName(entity.getName());
        response.setCode(entity.getCode());
        response.setDescription(entity.getDescription());
        response.setCapacity(entity.getCapacity());
        response.setDefaultRent(entity.getDefaultRent());
        response.setDefaultDeposit(entity.getDefaultDeposit());
        response.setStatus(entity.getStatus());
        response.setPrimaryImage(primaryImage);
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
