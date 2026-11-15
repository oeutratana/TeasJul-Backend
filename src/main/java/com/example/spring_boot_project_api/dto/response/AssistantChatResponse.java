package com.example.spring_boot_project_api.dto.response;

import java.util.List;

import com.example.spring_boot_project_api.service.assistant.dto.AiToolCall;

public record AssistantChatResponse(
        String reply,
        List<AiToolCall> toolCalls,
        boolean error
) {
}