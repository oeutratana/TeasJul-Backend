package com.example.spring_boot_project_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A previous assistant turn sent by the client to keep conversation context. */
public record AssistantHistoryMessage(
        @NotBlank(message = "History content is required")
        @Size(max = 4000, message = "History content is too long")
        String content,

        @NotBlank(message = "History role is required")
        String role
) {
}