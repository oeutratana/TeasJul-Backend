package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.CreateSystemDefaultRequest;
import com.example.spring_boot_project_api.dto.response.SystemDefaultResponse;
import com.example.spring_boot_project_api.dto.response.SystemDefaultViewResponse;
import com.example.spring_boot_project_api.enums.OwnerSystemDefaultStatus;
import com.example.spring_boot_project_api.enums.SystemDefaultKind;
import com.example.spring_boot_project_api.enums.SystemDefaultStatus;
import com.example.spring_boot_project_api.model.OwnerSystemDefault;
import com.example.spring_boot_project_api.model.SystemDefault;

@Component
public class SystemDefaultMapper {

    public SystemDefaultResponse toResponse(SystemDefault entity) {
        return new SystemDefaultResponse(
                entity.getId(),
                entity.getKind(),
                entity.getName(),
                entity.getCode(),
                entity.getUnit(),
                entity.getBillingMethod(),
                entity.getDescription(),
                entity.getIcon(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public SystemDefault toEntity(CreateSystemDefaultRequest request) {
        return SystemDefault.builder()
                .kind(request.kind())
                .name(request.name())
                .code(request.code())
                .unit(request.unit())
                .billingMethod(request.billingMethod())
                .description(request.description())
                .icon(request.icon())
                .status(SystemDefaultStatus.ACTIVE)
                .build();
    }

    public SystemDefaultViewResponse toViewResponse(SystemDefault definition,
            OwnerSystemDefault assignment) {
        boolean enabled = assignment != null
                && assignment.getStatus() == OwnerSystemDefaultStatus.ENABLED;
        Long linkedId = assignment == null ? null
                : (definition.getKind() == SystemDefaultKind.UTILITY
                        ? assignment.getUtilityTypeId()
                        : assignment.getAmenityId());
        OwnerSystemDefaultStatus assignmentStatus = assignment == null
                ? null
                : assignment.getStatus();
        return new SystemDefaultViewResponse(toResponse(definition), enabled,
                assignmentStatus, linkedId);
    }
}