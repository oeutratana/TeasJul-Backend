package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.GrantHouseAccessRequest;
import com.example.spring_boot_project_api.dto.response.HouseResponse;
import com.example.spring_boot_project_api.dto.response.UserHouseAccessResponse;
import com.example.spring_boot_project_api.dto.response.UserResponse;

public interface UserHouseAccessService {

    UserHouseAccessResponse grantAccess(Long houseId, GrantHouseAccessRequest request);

    void revokeAccess(Long houseId, Long userId);

    boolean hasAccess(Long userId, Long houseId);

    List<UserHouseAccessResponse> getHouseUsers(Long houseId);

    List<UserResponse> getAccessCandidates(Long houseId);

    List<HouseResponse> getUserHouses();
}
