package com.example.spring_boot_project_api.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.CreateMaintenanceCostRequest;
import com.example.spring_boot_project_api.dto.request.UpdateMaintenanceCostRequest;
import com.example.spring_boot_project_api.dto.response.MaintenanceCostResponse;
import com.example.spring_boot_project_api.exception.MaintenanceCostNotFoundException;
import com.example.spring_boot_project_api.exception.MaintenanceRequestNotFoundException;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.mapper.MaintenanceCostMapper;
import com.example.spring_boot_project_api.model.MaintenanceCost;
import com.example.spring_boot_project_api.model.MaintenanceCostType;
import com.example.spring_boot_project_api.model.MaintenanceRequest;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.MaintenanceCostRepository;
import com.example.spring_boot_project_api.repository.MaintenanceRequestRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.service.MaintenanceCostService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MaintenanceCostServiceImpl implements MaintenanceCostService {

    private static final int TOTAL_AMOUNT_SCALE = 2;

    private final MaintenanceCostRepository costRepository;
    private final MaintenanceRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final MaintenanceCostMapper costMapper;
    private final HouseAccessService houseAccessService;

    @Override
    @Transactional
    public MaintenanceCostResponse createCost(Long requestId, CreateMaintenanceCostRequest request) {
        User owner = getCurrentOwner();
        MaintenanceRequest maintenanceRequest = getOwnedRequest(requestId, owner.getId());

        MaintenanceCost cost = costMapper.toEntity(request);
        cost.setOwner(owner);
        cost.setRequest(maintenanceRequest);
        if (cost.getCostType() == null) {
            cost.setCostType(MaintenanceCostType.OTHER);
        }
        calculateTotalAmount(cost);

        return costMapper.toResponse(costRepository.save(cost));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceCostResponse> getCosts(Long requestId) {
        User owner = getCurrentOwner();
        getOwnedRequest(requestId, owner.getId());

        return costRepository.findAllByRequestIdOrderByCreatedAtAsc(requestId).stream()
                .map(costMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public MaintenanceCostResponse updateCost(Long requestId, Long costId, UpdateMaintenanceCostRequest request) {
        User owner = getCurrentOwner();
        getOwnedRequest(requestId, owner.getId());

        MaintenanceCost cost = costRepository.findByIdAndRequestId(costId, requestId)
                .orElseThrow(() -> MaintenanceCostNotFoundException.forId(costId));

        costMapper.applyUpdate(cost, request);
        calculateTotalAmount(cost);

        return costMapper.toResponse(costRepository.save(cost));
    }

    @Override
    @Transactional
    public void deleteCost(Long requestId, Long costId) {
        User owner = getCurrentOwner();
        getOwnedRequest(requestId, owner.getId());

        MaintenanceCost cost = costRepository.findByIdAndRequestId(costId, requestId)
                .orElseThrow(() -> MaintenanceCostNotFoundException.forId(costId));

        costRepository.delete(cost);
    }

    private void calculateTotalAmount(MaintenanceCost cost) {
        BigDecimal total = cost.getQuantity().multiply(cost.getUnitPrice())
                .setScale(TOTAL_AMOUNT_SCALE, RoundingMode.HALF_UP);
        cost.setTotalAmount(total);
    }

    private MaintenanceRequest getOwnedRequest(Long requestId, Long ownerId) {
        MaintenanceRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> MaintenanceRequestNotFoundException.forId(requestId));
        if (req.getOwner().getId().equals(ownerId)) {
            return req;
        }
        if (houseAccessService.canAccessHouse(req.getHouse().getId())) {
            return req;
        }
        throw MaintenanceRequestNotFoundException.forId(requestId);
    }

    private User getCurrentOwner() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserDetails userDetails = (UserDetails) principal;

        return userRepository.findByUsername(userDetails.getUsername())
                .or(() -> userRepository.findByEmail(userDetails.getUsername()))
                .orElseThrow(() -> UserNotFoundException.forId(null));
    }
}
