package com.example.spring_boot_project_api.exception;

public class ContractRenewalNotFoundException extends RuntimeException {

    public ContractRenewalNotFoundException(String message) {
        super(message);
    }

    public static ContractRenewalNotFoundException forId(Long id) {
        return new ContractRenewalNotFoundException("Contract renewal not found with id: " + id);
    }
}
