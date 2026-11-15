package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.PropertyOwnerRequest;
import com.example.spring_boot_project_api.dto.response.PropertyOwnerResponse;
import com.example.spring_boot_project_api.model.PropertyOwner;

@Component
public class PropertyOwnerMapper {

    public PropertyOwner toEntity(PropertyOwnerRequest request) {
        if (request == null) {
            return null;
        }
        return PropertyOwner.builder()
                .name(request.name())
                .email(request.email())
                .phone(request.phone())
                .address(request.address())
                .notes(request.notes())
                .build();
    }

    /** Copies writable fields onto an existing entity (no id/timestamps). */
    public void updateEntity(PropertyOwner entity, PropertyOwnerRequest request) {
        if (request == null) {
            return;
        }
        entity.setName(request.name());
        entity.setEmail(request.email());
        entity.setPhone(request.phone());
        entity.setAddress(request.address());
        entity.setNotes(request.notes());
    }

    public PropertyOwnerResponse toResponse(PropertyOwner entity) {
        if (entity == null) {
            return null;
        }
        return new PropertyOwnerResponse(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getAddress(),
                entity.getNotes(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}