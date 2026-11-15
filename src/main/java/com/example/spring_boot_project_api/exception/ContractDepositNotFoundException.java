package com.example.spring_boot_project_api.exception;

public class ContractDepositNotFoundException extends RuntimeException {

    public ContractDepositNotFoundException(String message) {
        super(message);
    }

    public static ContractDepositNotFoundException forId(Long id) {
        return new ContractDepositNotFoundException("Deposit not found with id: " + id);
    }
}
