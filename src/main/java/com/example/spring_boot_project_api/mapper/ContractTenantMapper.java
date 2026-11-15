package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.ContractTenantRequest;
import com.example.spring_boot_project_api.dto.response.ContractTenantResponse;
import com.example.spring_boot_project_api.model.ContractTenant;

@Component
public class ContractTenantMapper {
    public ContractTenantResponse toResponse(ContractTenant entity) {
        if (entity == null) {
            return null;
        }

        return new ContractTenantResponse(
                entity.getId(),
                entity.getContractId(),
                entity.getTenantId(),
                entity.getIsPrimary(),
                entity.getJoinedAt(),
                entity.getLeftAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public ContractTenant toEntity(ContractTenantRequest request) {
        if (request == null) {
            return null;
        }

        return ContractTenant.builder()
                .contractId(request.contractId())
                .tenantId(request.tenantId())
                .isPrimary(request.isPrimary() != null ? request.isPrimary() : false)
                .joinedAt(request.joinedAt())
                .leftAt(request.leftAt())
                .build();
    }

    public void updateEntityFromDto(ContractTenantRequest request, ContractTenant entity) {
        if (request == null || entity == null) {
            return;
        }

        entity.setContractId(request.contractId());
        entity.setTenantId(request.tenantId());
        if (request.isPrimary() != null) {
            entity.setIsPrimary(request.isPrimary());
        }
        entity.setJoinedAt(request.joinedAt());
        entity.setLeftAt(request.leftAt());
    }
}
