package com.example.spring_boot_project_api.mapper;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.CreateMaintenanceCostRequest;
import com.example.spring_boot_project_api.dto.request.UpdateMaintenanceCostRequest;
import com.example.spring_boot_project_api.dto.response.MaintenanceCostResponse;
import com.example.spring_boot_project_api.model.MaintenanceCost;

@Component
public class MaintenanceCostMapper {

    public MaintenanceCostResponse toResponse(MaintenanceCost cost) {
        return new MaintenanceCostResponse(
                cost.getId(),
                cost.getRequest().getId(),
                cost.getCostType(),
                cost.getDescription(),
                cost.getQuantity(),
                cost.getUnitPrice(),
                cost.getTotalAmount(),
                cost.getCreatedAt());
    }

    public MaintenanceCost toEntity(CreateMaintenanceCostRequest request) {
        return MaintenanceCost.builder()
                .description(request.description())
                .quantity(request.quantity())
                .unitPrice(request.unitPrice())
                .build();
    }

    public void applyUpdate(MaintenanceCost cost, UpdateMaintenanceCostRequest request) {
        if (request.costType() != null) {
            cost.setCostType(request.costType());
        }
        if (request.description() != null) {
            cost.setDescription(request.description());
        }
        BigDecimal quantity = request.quantity() != null ? request.quantity() : cost.getQuantity();
        BigDecimal unitPrice = request.unitPrice() != null ? request.unitPrice() : cost.getUnitPrice();
        cost.setQuantity(quantity);
        cost.setUnitPrice(unitPrice);
    }
}
