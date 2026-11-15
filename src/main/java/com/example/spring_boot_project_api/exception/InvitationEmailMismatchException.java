package com.example.spring_boot_project_api.exception;

public class InvitationEmailMismatchException extends RuntimeException {

    public InvitationEmailMismatchException(String message) {
        super(message);
    }
}