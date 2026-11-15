package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.CreateRenewalRequest;
import com.example.spring_boot_project_api.dto.response.ContractRenewalResponse;

public interface ContractRenewalService {

    ContractRenewalResponse createRenewal(CreateRenewalRequest request);

    ContractRenewalResponse getRenewalById(Long id);

    List<ContractRenewalResponse> getRenewalsByContract(Long contractId);
}
