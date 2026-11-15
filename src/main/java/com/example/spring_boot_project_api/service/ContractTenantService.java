package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.ContractTenantRequest;
import com.example.spring_boot_project_api.dto.response.ContractTenantResponse;

public interface ContractTenantService {
    ContractTenantResponse addTenantToContract(ContractTenantRequest request);

    ContractTenantResponse getContractTenantById(Long id);

    List<ContractTenantResponse> getTenantsByContract(Long contractId);

    List<ContractTenantResponse> getContractsByTenant(Long tenantId);

    ContractTenantResponse updateContractTenant(Long id, ContractTenantRequest request);

    void removeTenantFromContract(Long id);
}
