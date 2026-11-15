package com.example.spring_boot_project_api.exception;

/** Thrown when the AI assistant is disabled via {@code app.ai.enabled=false}. */
public class AssistantDisabledException extends RuntimeException {

    public AssistantDisabledException() {
        super("The AI assistant is currently disabled.");
    }
}