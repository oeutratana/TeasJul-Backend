package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.CreateUtilityTypeRequest;
import com.example.spring_boot_project_api.dto.response.UtilityTypeResponse;
import com.example.spring_boot_project_api.enums.UtilityTypeStatus;
import com.example.spring_boot_project_api.model.UtilityType;

@Component
public class UtilityTypeMapper {

    public UtilityTypeResponse toResponse(UtilityType type) {
        return new UtilityTypeResponse(
                type.getId(),
                type.getOwner().getId(),
                type.getName(),
                type.getCode(),
                type.getUnit(),
                type.getBillingMethod(),
                type.getStatus(),
                type.getCreatedAt(),
                type.getUpdatedAt());
    }

    public UtilityType toEntity(CreateUtilityTypeRequest request) {
        return UtilityType.builder()
                .name(request.name())
                .code(request.code())
                .unit(request.unit())
                .billingMethod(request.billingMethod() != null
                        ? request.billingMethod()
                        : com.example.spring_boot_project_api.enums.UtilityBillingMethod.METERED)
                .status(UtilityTypeStatus.ACTIVE)
                .build();
    }
}
