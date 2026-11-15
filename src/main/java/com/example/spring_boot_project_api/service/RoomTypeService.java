package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.RoomTypeRequest;
import com.example.spring_boot_project_api.dto.response.RoomTypeResponse;
import com.example.spring_boot_project_api.model.RoomType;

public interface RoomTypeService {

    RoomTypeResponse createRoomType(RoomTypeRequest request);

    RoomTypeResponse getRoomTypeById(Long id);

    List<RoomTypeResponse> getAllRoomTypes();

    List<RoomTypeResponse> getRoomTypesByOwner(Long ownerId);

    RoomTypeResponse updateRoomType(Long id, RoomTypeRequest request);

    void deleteRoomType(Long id);

    /**
     * Returns the room type after verifying the current user owns it or is a
     * manager scoped to its owner. Throws {@link org.springframework.security.access.AccessDeniedException}
     * when the user has no access, or {@link com.example.spring_boot_project_api.exception.RoomTypeNotFoundException}
     * when it does not exist.
     */
    RoomType requireAccessibleRoomType(Long id);
}
