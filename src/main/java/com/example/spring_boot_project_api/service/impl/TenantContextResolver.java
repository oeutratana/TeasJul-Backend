package com.example.spring_boot_project_api.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.enums.RentalContractStatus;
import com.example.spring_boot_project_api.exception.TenantNotFoundException;
import com.example.spring_boot_project_api.model.ContractTenant;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.repository.ContractTenantRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.TenantRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TenantContextResolver {

    private final TenantRepository tenantRepository;
    private final ContractTenantRepository contractTenantRepository;
    private final RentalContractRepository rentalContractRepository;

    public Tenant linkedTenantOf(Long userId) {
        if (userId == null) {
            return null;
        }
        return tenantRepository.findByUserId(userId).orElse(null);
    }

    public Tenant requireLinkedTenant(Long userId) {
        Tenant tenant = linkedTenantOf(userId);
        if (tenant == null) {
            throw new TenantNotFoundException("No tenant profile is linked to your account.");
        }
        return tenant;
    }

    public RentalContract activeContractOf(Long tenantId) {
        LocalDate today = LocalDate.now();
        List<ContractTenant> links = contractTenantRepository.findByTenantId(tenantId);

        RentalContract current = null;
        ContractTenant currentLink = null;

        for (ContractTenant link : links) {
            if (link.getLeftAt() != null && link.getLeftAt().isBefore(today)) {
                continue;
            }
            RentalContract contract = rentalContractRepository.findById(link.getContractId()).orElse(null);
            if (contract == null || contract.getStatus() != RentalContractStatus.ACTIVE) {
                continue;
            }
            if (current == null
                    || link.getJoinedAt().isAfter(currentLink.getJoinedAt())
                    || (link.getJoinedAt().equals(currentLink.getJoinedAt()) && link.getId() > currentLink.getId())) {
                current = contract;
                currentLink = link;
            }
        }
        return current;
    }
}