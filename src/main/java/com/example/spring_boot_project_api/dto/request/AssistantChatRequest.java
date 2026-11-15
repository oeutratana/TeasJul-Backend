package com.example.spring_boot_project_api.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssistantChatRequest(
        @NotBlank(message = "Message is required") @Size(max = 4000, message = "Message is too long")
        String message,

        String sessionId,

        @Valid List<AssistantHistoryMessage> history
) {
}