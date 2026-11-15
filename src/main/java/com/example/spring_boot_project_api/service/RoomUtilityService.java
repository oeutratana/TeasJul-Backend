package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.BulkRoomUtilityRequest;
import com.example.spring_boot_project_api.dto.response.BulkAssignmentResponse;
import com.example.spring_boot_project_api.dto.response.RoomUtilityResponse;

public interface RoomUtilityService {

    List<RoomUtilityResponse> getRoomUtilities(Long roomId);

    RoomUtilityResponse assignUtility(Long roomId, Long utilityTypeId, Boolean isIncluded);

    void removeUtility(Long roomId, Long utilityTypeId);

    BulkAssignmentResponse bulkAssignUtilities(BulkRoomUtilityRequest request);
}
