package com.example.spring_boot_project_api.dto.response;

/**
 * Generic success message response with a status flag. Used for actions that
 * do not return a body of their own.
 */
public record MessageResponse(
        boolean success,
        String message
) {
    public static MessageResponse of(String message) {
        return new MessageResponse(true, message);
    }
}
