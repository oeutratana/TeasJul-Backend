package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.CreateDepositRequest;
import com.example.spring_boot_project_api.dto.request.UpdateDepositPaymentRequest;
import com.example.spring_boot_project_api.dto.response.ContractDepositResponse;

public interface ContractDepositService {
    ContractDepositResponse createDeposit(CreateDepositRequest request);

    ContractDepositResponse markAsPaid(Long depositId, UpdateDepositPaymentRequest request);

    List<ContractDepositResponse> getDepositsByContract(Long contractId);

    ContractDepositResponse getDepositById(Long depositId);
}
