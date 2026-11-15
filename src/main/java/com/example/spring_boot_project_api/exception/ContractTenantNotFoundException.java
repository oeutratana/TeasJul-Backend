package com.example.spring_boot_project_api.exception;

public class ContractTenantNotFoundException extends RuntimeException {

    public ContractTenantNotFoundException(String message) {
        super(message);
    }

    public static ContractTenantNotFoundException forId(Long id) {
        return new ContractTenantNotFoundException("Contract tenant relationship not found with id: " + id);
    }
}
