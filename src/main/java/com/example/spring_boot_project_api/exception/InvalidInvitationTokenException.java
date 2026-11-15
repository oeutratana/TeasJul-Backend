package com.example.spring_boot_project_api.exception;

public class InvalidInvitationTokenException extends RuntimeException {

    public InvalidInvitationTokenException(String message) {
        super(message);
    }
}