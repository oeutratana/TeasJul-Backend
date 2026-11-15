package com.example.spring_boot_project_api.exception;

public class InvitationExpiredException extends RuntimeException {

    public InvitationExpiredException(String message) {
        super(message);
    }
}