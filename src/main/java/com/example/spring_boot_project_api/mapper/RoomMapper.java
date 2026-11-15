package com.example.spring_boot_project_api.mapper;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.RoomRepuest;
import com.example.spring_boot_project_api.dto.response.RoomResponse;
import com.example.spring_boot_project_api.dto.response.RoomTenantResponse;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.RoomType;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.RoomTypeRepository;

@Component
public class RoomMapper {

    @Autowired
    private HouseRepository houseRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    public Room toEntity(RoomRepuest dto) {
        if (dto == null) return null;

        Room room = new Room();
        room.setHouseId(dto.getHouseId());
        room.setRoomTypeId(dto.getRoomTypeId());
        room.setRoomNumber(dto.getRoomNumber());
        room.setName(dto.getName());
        room.setFloorNumber(dto.getFloorNumber());
        room.setMonthlyRent(dto.getMonthlyRent() != null ? dto.getMonthlyRent() : BigDecimal.ZERO);
        room.setSecurityDeposit(dto.getSecurityDeposit() != null ? dto.getSecurityDeposit() : BigDecimal.ZERO);
        room.setCapacity(dto.getCapacity() != null ? dto.getCapacity() : 1);
        room.setStatus(dto.getStatus() != null ? dto.getStatus() : RoomStatus.AVAILABLE);
        room.setDescription(dto.getDescription());
        room.setPropertyType(dto.getPropertyType());
        room.setListingScope(dto.getListingScope());
        room.setFurnishing(dto.getFurnishing());
        room.setAvailableFrom(dto.getAvailableFrom());
        room.setAdvancePayment(dto.getAdvancePayment());
        room.setBedrooms(dto.getBedrooms());
        room.setBathrooms(dto.getBathrooms());
        room.setAreaSqm(dto.getAreaSqm());
        room.setLandSizeSqm(dto.getLandSizeSqm());
        room.setNumberOfFloors(dto.getNumberOfFloors());
        return room;
    }

    public RoomResponse toDto(Room entity) {
        return toDto(entity, null);
    }

    public RoomResponse toDto(Room entity, RoomTenantResponse tenant) {
        if (entity == null) return null;

        RoomResponse response = new RoomResponse();
        response.setId(entity.getId());
        response.setOwnerId(entity.getOwnerId());
        response.setHouseId(entity.getHouseId());
        response.setRoomTypeId(entity.getRoomTypeId());
        response.setRoomNumber(entity.getRoomNumber());
        response.setName(entity.getName());
        response.setFloorNumber(entity.getFloorNumber());
        response.setMonthlyRent(entity.getMonthlyRent());
        response.setSecurityDeposit(entity.getSecurityDeposit());
        response.setCapacity(entity.getCapacity());
        response.setStatus(entity.getStatus());
        response.setDescription(entity.getDescription());
        response.setPropertyType(entity.getPropertyType());
        response.setListingScope(entity.getListingScope());
        response.setFurnishing(entity.getFurnishing());
        response.setAvailableFrom(entity.getAvailableFrom());
        response.setAdvancePayment(entity.getAdvancePayment());
        response.setBedrooms(entity.getBedrooms());
        response.setBathrooms(entity.getBathrooms());
        response.setAreaSqm(entity.getAreaSqm());
        response.setLandSizeSqm(entity.getLandSizeSqm());
        response.setNumberOfFloors(entity.getNumberOfFloors());
        response.setListingId(entity.getListingId());
        response.setHouseName(resolveHouseName(entity.getHouseId()));
        response.setRoomTypeName(resolveRoomTypeName(entity.getRoomTypeId()));
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        response.setTenant(tenant);

        response.setPublicationStatus(entity.getPublicationStatus());
        response.setSubmittedAt(entity.getSubmittedAt());
        response.setSubmittedBy(entity.getSubmittedBy());
        response.setApprovedAt(entity.getApprovedAt());
        response.setApprovedBy(entity.getApprovedBy());
        response.setPublishedAt(entity.getPublishedAt());
        response.setRejectedAt(entity.getRejectedAt());
        response.setRejectedBy(entity.getRejectedBy());
        response.setRejectionReason(entity.getRejectionReason());
        return response;
    }

    private String resolveHouseName(Long houseId) {
        if (houseId == null || houseRepository == null) return null;
        return houseRepository.findById(houseId)
                .map(House::getName)
                .orElse(null);
    }

    private String resolveRoomTypeName(Long roomTypeId) {
        if (roomTypeId == null || roomTypeRepository == null) return null;
        return roomTypeRepository.findById(roomTypeId)
                .map(RoomType::getName)
                .orElse(null);
    }
}
