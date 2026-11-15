package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.BulkRoomAmenityRequest;
import com.example.spring_boot_project_api.dto.response.BulkAssignmentResponse;
import com.example.spring_boot_project_api.dto.response.RoomAmenityResponse;

public interface RoomAmenityService {

    RoomAmenityResponse assignAmenityToRoom(Long roomId, Long amenityId);

    List<RoomAmenityResponse> getRoomAmenities(Long roomId);

    void removeAmenityFromRoom(Long roomId, Long amenityId);

    BulkAssignmentResponse bulkAssignAmenities(BulkRoomAmenityRequest request);
}
