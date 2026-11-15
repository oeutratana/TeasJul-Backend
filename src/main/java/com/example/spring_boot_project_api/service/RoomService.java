package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.RoomRepuest;
import com.example.spring_boot_project_api.dto.response.RoomResponse;

public interface RoomService {

    List<RoomResponse> getAllRooms();

    RoomResponse getRoomById(Long id);

    List<RoomResponse> getRoomsByHouse(Long houseId);

    RoomResponse createRoom(RoomRepuest requestDto);

    RoomResponse updateRoom(Long id, RoomRepuest requestDto);

    void deleteRoom(Long id);
}
