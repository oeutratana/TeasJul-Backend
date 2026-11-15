package com.example.spring_boot_project_api.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.exception.HouseNotFoundException;
import com.example.spring_boot_project_api.exception.InvoiceNotFoundException;
import com.example.spring_boot_project_api.exception.RentalContractNotFoundException;
import com.example.spring_boot_project_api.exception.RoomNotFoundException;
import com.example.spring_boot_project_api.exception.TenantNotFoundException;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.Invoice;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.InvoiceRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HouseAccessServiceImpl implements HouseAccessService {

    private final HouseRepository houseRepository;
    private final RoomRepository roomRepository;
    private final RentalContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;
    private final TenantRepository tenantRepository;
    private final UserHouseAccessRepository userHouseAccessRepository;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional(readOnly = true)
    public boolean canAccessHouse(Long houseId) {
        Long currentUserId = securityUtils.getCurrentUserId();

        House house = houseRepository.findById(houseId).orElse(null);
        if (house == null) {
            return false;
        }

        if (house.getOwner() != null && house.getOwner().getId().equals(currentUserId)) {
            return true;
        }

        if (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)
                && userHouseAccessRepository.existsByUserIdAndHouseId(currentUserId, houseId)) {
            return true;
        }

        if (securityUtils.hasRole(SecurityUtils.ROLE_SUPER_ADMIN)) {
            return true;
        }

        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canManageHouse(Long houseId) {
        return canAccessHouse(houseId);
    }

    @Override
    @Transactional(readOnly = true)
    public void requireHouseAccess(Long houseId) {
        if (!canAccessHouse(houseId)) {
            throw new AccessDeniedException("You do not have permission to access this resource");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void requireOwnerHouseAccess(Long houseId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> HouseNotFoundException.forId(houseId));
        if (house.getOwner() == null || !house.getOwner().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to access this resource");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Long resolveRoomHouseId(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> RoomNotFoundException.forId(roomId));
        requireHouseAccess(room.getHouseId());
        return room.getHouseId();
    }

    @Override
    @Transactional(readOnly = true)
    public Long resolveContractHouseId(Long contractId) {
        RentalContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> RentalContractNotFoundException.forId(contractId));
        Room room = roomRepository.findById(contract.getRoomId())
                .orElseThrow(() -> RoomNotFoundException.forId(contract.getRoomId()));
        requireHouseAccess(room.getHouseId());
        return room.getHouseId();
    }

    @Override
    @Transactional(readOnly = true)
    public Long resolveInvoiceHouseId(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> InvoiceNotFoundException.forId(invoiceId));
        RentalContract contract = contractRepository.findById(invoice.getContractId()).orElse(null);
        if (contract == null) {
            throw RentalContractNotFoundException.forId(invoice.getContractId());
        }
        Room room = roomRepository.findById(contract.getRoomId()).orElse(null);
        if (room == null) {
            throw RoomNotFoundException.forId(contract.getRoomId());
        }
        requireHouseAccess(room.getHouseId());
        return room.getHouseId();
    }

    @Override
    @Transactional(readOnly = true)
    public Long resolveTenantHouseId(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> TenantNotFoundException.forId(tenantId));
        if (tenant.getHouse() != null) {
            requireHouseAccess(tenant.getHouse().getId());
            return tenant.getHouse().getId();
        }
        if (tenant.getOwner() != null) {
            House house = houseRepository.findAllByOwnerIdOrderByCreatedAtDesc(tenant.getOwner().getId())
                    .stream().findFirst().orElse(null);
            if (house != null) {
                requireHouseAccess(house.getId());
                return house.getId();
            }
        }
        throw new AccessDeniedException("You do not have permission to access this resource");
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getAccessibleHouseIds() {
        Long currentUserId = securityUtils.getCurrentUserId();

        if (securityUtils.hasRole(SecurityUtils.ROLE_OWNER)) {
            List<Long> ownerHouseIds = new ArrayList<>(
                    houseRepository.findAllByOwnerIdOrderByCreatedAtDesc(currentUserId).stream()
                            .map(House::getId)
                            .toList());
            if (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
                List<Long> managedHouseIds = userHouseAccessRepository.findHouseIdsByUserId(currentUserId);
                for (Long hId : managedHouseIds) {
                    if (!ownerHouseIds.contains(hId)) {
                        ownerHouseIds.add(hId);
                    }
                }
            }
            return ownerHouseIds;
        }

        if (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
            return userHouseAccessRepository.findHouseIdsByUserId(currentUserId);
        }

        return Collections.emptyList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getScopedOwnerIds() {
        Long currentUserId = securityUtils.getCurrentUserId();
        List<Long> ownerIds = new ArrayList<>();

        if (securityUtils.hasRole(SecurityUtils.ROLE_OWNER)) {
            ownerIds.add(currentUserId);
        }

        if (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
            List<Long> managedOwnerIds = userHouseAccessRepository.findOwnerIdsByUserId(currentUserId);
            for (Long ownerId : managedOwnerIds) {
                if (!ownerIds.contains(ownerId)) {
                    ownerIds.add(ownerId);
                }
            }
        }

        return ownerIds;
    }

    @Override
    public boolean isOwner() {
        return securityUtils.hasRole(SecurityUtils.ROLE_OWNER);
    }

    @Override
    public boolean isManager() {
        return securityUtils.hasRole(SecurityUtils.ROLE_MANAGER);
    }
}
