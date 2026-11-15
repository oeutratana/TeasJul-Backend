package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.AdminBulkCreateRoomRequest;
import com.example.spring_boot_project_api.dto.request.AdminCreateRoomRequest;
import com.example.spring_boot_project_api.dto.request.CreateHouseRequest;
import com.example.spring_boot_project_api.dto.request.RoomTypeRequest;
import com.example.spring_boot_project_api.dto.response.HouseResponse;
import com.example.spring_boot_project_api.dto.response.RoomResponse;
import com.example.spring_boot_project_api.dto.response.RoomTypeResponse;

public interface AdminRoomService {

    List<HouseResponse> getOwnerHouses(Long ownerId);

    List<RoomTypeResponse> getOwnerRoomTypes(Long ownerId);

    RoomResponse createRoomForOwner(Long ownerId, AdminCreateRoomRequest request);

    List<RoomResponse> createRoomsForOwner(Long ownerId, AdminBulkCreateRoomRequest request);

    HouseResponse createHouseForOwner(Long ownerId, CreateHouseRequest request);

    RoomTypeResponse createRoomTypeForOwner(Long ownerId, RoomTypeRequest request);
}