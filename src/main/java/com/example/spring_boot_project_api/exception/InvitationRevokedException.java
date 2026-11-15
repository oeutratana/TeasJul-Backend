package com.example.spring_boot_project_api.exception;

public class InvitationRevokedException extends RuntimeException {

    public InvitationRevokedException(String message) {
        super(message);
    }
}