package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.CreateTenantEmergencyContactRequest;
import com.example.spring_boot_project_api.dto.response.TenantEmergencyContactResponse;
import com.example.spring_boot_project_api.model.TenantEmergencyContact;

@Component
public class TenantEmergencyContactMapper {

    public TenantEmergencyContactResponse toResponse(TenantEmergencyContact contact) {
        return new TenantEmergencyContactResponse(
                contact.getId(),
                contact.getTenant().getId(),
                contact.getName(),
                contact.getRelationship(),
                contact.getPhone(),
                contact.getAddress(),
                contact.getIsPrimary(),
                contact.getCreatedAt(),
                contact.getUpdatedAt());
    }

    public TenantEmergencyContact toEntity(CreateTenantEmergencyContactRequest request) {
        return TenantEmergencyContact.builder()
                .name(request.name())
                .relationship(request.relationship())
                .phone(request.phone())
                .address(request.address())
                .isPrimary(request.isPrimary() != null ? request.isPrimary() : false)
                .build();
    }
}
