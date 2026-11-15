package com.example.spring_boot_project_api.service.impl;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.UpdateTenantRequest;
import com.example.spring_boot_project_api.dto.response.TenantMeResponse;
import com.example.spring_boot_project_api.dto.response.TenantUtilityResponse;
import com.example.spring_boot_project_api.enums.UtilityMeterStatus;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.mapper.TenantPortalMapper;
import com.example.spring_boot_project_api.model.ContractDeposit;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UtilityMeter;
import com.example.spring_boot_project_api.model.UtilityReading;
import com.example.spring_boot_project_api.model.UtilityType;
import com.example.spring_boot_project_api.repository.ContractDepositRepository;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.repository.UtilityMeterRepository;
import com.example.spring_boot_project_api.repository.UtilityReadingRepository;
import com.example.spring_boot_project_api.repository.UtilityTypeRepository;
import com.example.spring_boot_project_api.service.TenantPortalService;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TenantPortalServiceImpl implements TenantPortalService {

    private final TenantContextResolver tenantContextResolver;
    private final TenantRepository tenantRepository;
    private final RoomRepository roomRepository;
    private final HouseRepository houseRepository;
    private final ContractDepositRepository contractDepositRepository;
    private final UtilityMeterRepository utilityMeterRepository;
    private final UtilityReadingRepository utilityReadingRepository;
    private final UtilityTypeRepository utilityTypeRepository;
    private final SecurityUtils securityUtils;
    private final TenantPortalMapper tenantPortalMapper;

    @Override
    @Transactional(readOnly = true)
    public TenantMeResponse getMyTenantProfile() {
        Tenant tenant = requireMyTenant();
        RentalContract contract = tenantContextResolver.activeContractOf(tenant.getId());

        Room room = null;
        House house = null;
        if (contract != null) {
            room = roomRepository.findById(contract.getRoomId()).orElse(null);
            house = room != null ? houseRepository.findById(room.getHouseId()).orElse(null) : null;
        }

        BigDecimal depositPaid = BigDecimal.ZERO;
        boolean depositFullyPaid = false;
        if (contract != null) {
            for (ContractDeposit deposit : contractDepositRepository.findByRentalContractId(contract.getId())) {
                BigDecimal paid = deposit.getPaidAmount() == null ? BigDecimal.ZERO : deposit.getPaidAmount();
                depositPaid = depositPaid.add(paid);
                if (paid.compareTo(deposit.getAmount()) >= 0) {
                    depositFullyPaid = true;
                }
            }
        }

        return tenantPortalMapper.toMeResponse(tenant,
                tenantPortalMapper.toMeHouse(house),
                tenantPortalMapper.toMeRoom(room),
                tenantPortalMapper.toMeContract(contract, depositPaid, depositFullyPaid));
    }

    @Override
    @Transactional
    public TenantMeResponse updateMyTenantProfile(UpdateTenantRequest request) {
        Tenant tenant = requireMyTenant();

        if (request.phone() != null) {
            if (tenantRepository.existsByOwnerIdAndPhoneAndIdNot(tenant.getOwner().getId(),
                    request.phone(), tenant.getId())) {
                throw new DuplicateResourceException(
                        "Tenant phone already exists for this owner: " + request.phone());
            }
            tenant.setPhone(request.phone());
        }
        if (request.firstName() != null) {
            tenant.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            tenant.setLastName(request.lastName());
        }
        if (request.gender() != null) {
            tenant.setGender(request.gender());
        }
        if (request.dateOfBirth() != null) {
            tenant.setDateOfBirth(request.dateOfBirth());
        }
        if (request.email() != null) {
            tenant.setEmail(request.email());
        }
        if (request.occupationType() != null) {
            tenant.setOccupationType(request.occupationType());
        }
        if (request.occupation() != null) {
            tenant.setOccupation(request.occupation());
        }
        if (request.schoolOrCompany() != null) {
            tenant.setSchoolOrCompany(request.schoolOrCompany());
        }
        if (request.idType() != null) {
            tenant.setIdType(request.idType());
        }
        if (request.idNumber() != null) {
            tenant.setIdNumber(request.idNumber());
        }
        if (request.address() != null) {
            tenant.setAddress(request.address());
        }

        tenantRepository.save(tenant);
        return getMyTenantProfile();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantUtilityResponse> getMyUtilities() {
        Tenant tenant = requireMyTenant();
        RentalContract contract = tenantContextResolver.activeContractOf(tenant.getId());
        if (contract == null) {
            return List.of();
        }
        Room room = roomRepository.findById(contract.getRoomId()).orElse(null);
        if (room == null) {
            return List.of();
        }

        return utilityMeterRepository.findByRoomId(room.getId()).stream()
                .filter(meter -> meter.getStatus() == UtilityMeterStatus.ACTIVE)
                .map(meter -> {
                    UtilityReading latest = utilityReadingRepository
                            .findTopByMeterIdOrderByReadingDateDesc(meter.getId()).orElse(null);
                    UtilityType type = meter.getUtilityTypeId() != null
                            ? utilityTypeRepository.findById(meter.getUtilityTypeId()).orElse(null)
                            : null;
                    return tenantPortalMapper.toUtilityResponse(meter, type, latest);
                })
                .toList();
    }

    private Tenant requireMyTenant() {
        User me = securityUtils.getCurrentUser();
        return tenantContextResolver.requireLinkedTenant(me.getId());
    }
}