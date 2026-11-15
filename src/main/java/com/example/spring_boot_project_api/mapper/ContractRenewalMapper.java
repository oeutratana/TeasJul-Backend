package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.CreateRenewalRequest;
import com.example.spring_boot_project_api.dto.response.ContractRenewalResponse;
import com.example.spring_boot_project_api.model.ContractRenewal;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.User;

@Component
public class ContractRenewalMapper {

    public ContractRenewal toEntity(RentalContract rentalContract, User createdBy, CreateRenewalRequest request) {
        return ContractRenewal.builder()
                .rentalContract(rentalContract)
                .previousEndDate(request.previousEndDate())
                .newEndDate(request.newEndDate())
                .previousRent(request.previousRent())
                .newRent(request.newRent())
                .notes(request.notes())
                .createdBy(createdBy)
                .build();
    }

    public ContractRenewalResponse toResponse(ContractRenewal renewal) {
        User createdBy = renewal.getCreatedBy();

        return new ContractRenewalResponse(
                renewal.getId(),
                renewal.getRentalContract().getId(),
                renewal.getPreviousEndDate(),
                renewal.getNewEndDate(),
                renewal.getPreviousRent(),
                renewal.getNewRent(),
                renewal.getRenewedAt(),
                renewal.getNotes(),
                createdBy != null ? createdBy.getId() : null,
                createdBy != null ? createdBy.getUsername() : null);
    }
}
