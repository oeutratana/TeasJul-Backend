package com.example.spring_boot_project_api.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.ContractTenantRequest;
import com.example.spring_boot_project_api.dto.response.ContractTenantResponse;
import com.example.spring_boot_project_api.enums.RentalContractStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.exception.ContractTenantNotFoundException;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.RentalContractNotFoundException;
import com.example.spring_boot_project_api.exception.TenantNotFoundException;
import com.example.spring_boot_project_api.mapper.ContractTenantMapper;
import com.example.spring_boot_project_api.model.ContractTenant;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.model.TenantStatus;
import com.example.spring_boot_project_api.repository.ContractTenantRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.service.ContractTenantService;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.service.NotificationService;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ContractTenantServiceImpl implements ContractTenantService {
    private static final Logger log = LoggerFactory.getLogger(ContractTenantServiceImpl.class);

    private final ContractTenantRepository contractTenantRepository;
    private final RentalContractRepository contractRepository;
    private final RoomRepository roomRepository;
    private final TenantRepository tenantRepository;
    private final ContractTenantMapper contractTenantMapper;
    private final SecurityUtils securityUtils;
    private final HouseAccessService houseAccessService;
    private final NotificationService notificationService;

    @Override
    public ContractTenantResponse addTenantToContract(ContractTenantRequest request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        RentalContract contract = getOwnedContract(request.contractId());
        Tenant tenant = tenantRepository.findByIdAndOwnerId(request.tenantId(), currentUserId)
                .orElseThrow(() -> TenantNotFoundException.forId(request.tenantId()));

        if (tenant.getStatus() != TenantStatus.ACTIVE && tenant.getStatus() != TenantStatus.PENDING) {
            throw new DuplicateResourceException(
                    "Only ACTIVE or PENDING tenants can be assigned to a contract. Current status: "
                            + tenant.getStatus());
        }

        if (contractTenantRepository.existsByContractIdAndTenantId(request.contractId(), request.tenantId())) {
            throw new DuplicateResourceException("Tenant is already assigned to this contract.");
        }

        if (contractTenantRepository.isTenantActivelyRenting(request.tenantId(), LocalDate.now())) {
            throw new DuplicateResourceException("Tenant is currently renting another unit and cannot be assigned to a new contract.");
        }

        ContractTenant entity = contractTenantMapper.toEntity(request);
        ContractTenant saved = contractTenantRepository.save(entity);

        // Once a tenant is assigned, a draft contract becomes active automatically.
        if (contract.getStatus() == RentalContractStatus.DRAFT) {
            contract.setStatus(RentalContractStatus.ACTIVE);
            contractRepository.save(contract);
            roomRepository.findById(contract.getRoomId()).ifPresent(room -> {
                if (room.getStatus() == RoomStatus.AVAILABLE) {
                    room.setStatus(RoomStatus.OCCUPIED);
                    roomRepository.save(room);
                }
            });
        }

        syncTenantStatus(saved.getTenantId());
        try {
            notificationService.notifyRoomAssigned(contract, tenant);
        } catch (RuntimeException e) {
            log.warn("Failed to create room-assigned notification", e);
        }
        return contractTenantMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ContractTenantResponse getContractTenantById(Long id) {
        return contractTenantMapper.toResponse(getOwnedContractTenant(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractTenantResponse> getTenantsByContract(Long contractId) {
        getOwnedContract(contractId);
        return contractTenantRepository.findByContractId(contractId)
                .stream()
                .map(contractTenantMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractTenantResponse> getContractsByTenant(Long tenantId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        verifyTenantOwnership(tenantId, currentUserId);
        return contractTenantRepository.findByTenantId(tenantId)
                .stream()
                .map(contractTenantMapper::toResponse)
                .toList();
    }

    @Override
    public ContractTenantResponse updateContractTenant(Long id, ContractTenantRequest request) {
        ContractTenant existing = getOwnedContractTenant(id);
        Long currentUserId = securityUtils.getCurrentUserId();
        getOwnedContract(request.contractId());
        verifyTenantOwnership(request.tenantId(), currentUserId);

        contractTenantRepository.findByContractIdAndTenantId(request.contractId(), request.tenantId())
                .filter(ct -> !ct.getId().equals(id))
                .ifPresent(ct -> {
                    throw new DuplicateResourceException("Tenant is already assigned to this contract.");
                });

        contractTenantMapper.updateEntityFromDto(request, existing);
        ContractTenant saved = contractTenantRepository.save(existing);
        syncTenantStatus(saved.getTenantId());
        return contractTenantMapper.toResponse(saved);
    }

    @Override
    public void removeTenantFromContract(Long id) {
        ContractTenant existing = getOwnedContractTenant(id);
        Long tenantId = existing.getTenantId();
        contractTenantRepository.delete(existing);
        syncTenantStatus(tenantId);
    }

    public void syncTenantStatus(Long tenantId) {
        if (tenantId == null) return;
        tenantRepository.findById(tenantId).ifPresent(tenant -> {
            if (tenant.getStatus() == TenantStatus.INACTIVE) return;
            boolean isRenting = contractTenantRepository.isTenantActivelyRenting(tenantId, LocalDate.now());
            if (isRenting && tenant.getStatus() != TenantStatus.RENTING) {
                tenant.setStatus(TenantStatus.RENTING);
                tenantRepository.save(tenant);
            } else if (!isRenting && tenant.getStatus() == TenantStatus.RENTING) {
                tenant.setStatus(TenantStatus.ACTIVE);
                tenantRepository.save(tenant);
            }
        });
    }

    private ContractTenant getOwnedContractTenant(Long id) {
        return contractTenantRepository.findById(id)
                .map(ct -> {
                    getOwnedContract(ct.getContractId());
                    return ct;
                })
                .orElseThrow(() -> ContractTenantNotFoundException.forId(id));
    }

    private RentalContract getOwnedContract(Long contractId) {
        RentalContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> RentalContractNotFoundException.forId(contractId));
        houseAccessService.resolveContractHouseId(contractId);
        return contract;
    }

    private void verifyTenantOwnership(Long tenantId, Long currentUserId) {
        com.example.spring_boot_project_api.model.Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> TenantNotFoundException.forId(tenantId));
        if (tenant.getOwner().getId().equals(currentUserId)) {
            return;
        }
        if (tenant.getHouse() != null && houseAccessService.canAccessHouse(tenant.getHouse().getId())) {
            return;
        }
        throw TenantNotFoundException.forId(tenantId);
    }
}
