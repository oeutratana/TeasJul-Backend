package com.example.spring_boot_project_api.exception;

public class FeedbackNotFoundException extends RuntimeException {

    public FeedbackNotFoundException(String message) {
        super(message);
    }

    public static FeedbackNotFoundException forId(Long id) {
        return new FeedbackNotFoundException("Feedback not found with id: " + id);
    }
}