package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.CreateTenantRequest;
import com.example.spring_boot_project_api.dto.response.TenantResponse;
import com.example.spring_boot_project_api.model.OccupationType;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.model.TenantStatus;

@Component
public class TenantMapper {

    public TenantResponse toResponse(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getUser() != null ? tenant.getUser().getId() : null,
                tenant.getOwner().getId(),
                tenant.getHouse() != null ? tenant.getHouse().getId() : null,
                tenant.getInvitedBy() != null ? tenant.getInvitedBy().getId() : null,
                tenant.getTenantCode(),
                tenant.getFirstName(),
                tenant.getLastName(),
                tenant.getGender(),
                tenant.getDateOfBirth(),
                tenant.getPhone(),
                tenant.getEmail(),
                tenant.getOccupationType(),
                tenant.getOccupation(),
                tenant.getSchoolOrCompany(),
                tenant.getIdType(),
                tenant.getIdNumber(),
                tenant.getAddress(),
                tenant.getStatus(),
                tenant.getCreatedAt(),
                tenant.getUpdatedAt());
    }

    public Tenant toEntity(CreateTenantRequest request) {
        return Tenant.builder()
                .tenantCode(request.tenantCode())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .gender(request.gender())
                .dateOfBirth(request.dateOfBirth())
                .phone(request.phone())
                .email(request.email())
                .occupationType(request.occupationType() != null ? request.occupationType() : OccupationType.OTHER)
                .occupation(request.occupation())
                .schoolOrCompany(request.schoolOrCompany())
                .idType(request.idType())
                .idNumber(request.idNumber())
                .address(request.address())
                .status(TenantStatus.ACTIVE)
                .build();
    }
}
