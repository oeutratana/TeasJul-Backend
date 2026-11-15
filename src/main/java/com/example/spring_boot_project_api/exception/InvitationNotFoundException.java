package com.example.spring_boot_project_api.exception;

public class InvitationNotFoundException extends RuntimeException {

    public InvitationNotFoundException(String message) {
        super(message);
    }

    public static InvitationNotFoundException forId(Long id) {
        return new InvitationNotFoundException("Tenant invitation not found with id: " + id);
    }
}