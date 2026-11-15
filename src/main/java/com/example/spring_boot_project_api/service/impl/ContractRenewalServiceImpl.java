package com.example.spring_boot_project_api.service.impl;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.CreateRenewalRequest;
import com.example.spring_boot_project_api.dto.response.ContractRenewalResponse;
import com.example.spring_boot_project_api.exception.ContractRenewalNotFoundException;
import com.example.spring_boot_project_api.exception.RentalContractNotFoundException;
import com.example.spring_boot_project_api.mapper.ContractRenewalMapper;
import com.example.spring_boot_project_api.model.ContractRenewal;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.ContractRenewalRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.service.ContractRenewalService;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.service.RentalContractService;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ContractRenewalServiceImpl implements ContractRenewalService {

    private final ContractRenewalRepository renewalRepository;
    private final RentalContractRepository contractRepository;
    private final ContractRenewalMapper renewalMapper;
    private final SecurityUtils securityUtils;
    private final HouseAccessService houseAccessService;
    private final RentalContractService rentalContractService;

    @Override
    @Transactional
    public ContractRenewalResponse createRenewal(CreateRenewalRequest request) {
        RentalContract contract = getOwnedContract(request.contractId());
        User createdBy = securityUtils.getCurrentUser();

        rentalContractService.applyRenewal(contract.getId(), request.newEndDate());

        ContractRenewal saved = renewalRepository.save(renewalMapper.toEntity(contract, createdBy, request));
        return renewalMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ContractRenewalResponse getRenewalById(Long id) {
        return renewalMapper.toResponse(getOwnedRenewal(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractRenewalResponse> getRenewalsByContract(Long contractId) {
        getOwnedContract(contractId);
        return renewalRepository.findByRentalContractIdOrderByRenewedAtDesc(contractId)
                .stream()
                .map(renewalMapper::toResponse)
                .toList();
    }

    private ContractRenewal assertOwnedAndGet(ContractRenewal renewal) {
        houseAccessService.resolveContractHouseId(renewal.getRentalContract().getId());
        return renewal;
    }

    private ContractRenewal getOwnedRenewal(Long id) {
        ContractRenewal renewal = renewalRepository.findById(id)
                .orElseThrow(() -> ContractRenewalNotFoundException.forId(id));
        return assertOwnedAndGet(renewal);
    }

    private RentalContract getOwnedContract(Long contractId) {
        RentalContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> RentalContractNotFoundException.forId(contractId));
        houseAccessService.resolveContractHouseId(contractId);
        return contract;
    }
}
