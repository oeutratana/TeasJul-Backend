package com.example.spring_boot_project_api.exception;

public class RentalContractNotFoundException extends RuntimeException {

    public RentalContractNotFoundException(String message) {
        super(message);
    }

    public static RentalContractNotFoundException forId(Long id) {
        return new RentalContractNotFoundException("Rental contract not found with id: " + id);
    }
}
