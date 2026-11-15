package com.example.spring_boot_project_api.exception;

public class InvitationAlreadyUsedException extends RuntimeException {

    public InvitationAlreadyUsedException(String message) {
        super(message);
    }
}