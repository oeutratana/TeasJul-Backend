package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.UserHouseAccessResponse;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserHouseAccess;
import com.example.spring_boot_project_api.model.UserHouseAccessId;

@Component
public class UserHouseAccessMapper {

    public UserHouseAccess toEntity(User user, House house) {
        return UserHouseAccess.builder()
                .id(new UserHouseAccessId(user.getId(), house.getId()))
                .user(user)
                .house(house)
                .build();
    }

    public UserHouseAccessResponse toResponse(UserHouseAccess access) {
        return new UserHouseAccessResponse(
                access.getUser().getId(),
                access.getUser().getUsername(),
                access.getHouse().getId(),
                access.getCreatedAt());
    }
}
