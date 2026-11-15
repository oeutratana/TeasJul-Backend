package com.example.spring_boot_project_api.service.assistant.dto;

import java.util.List;

public record AiPrediction(String content, List<AiToolCall> toolCalls) {

    public static AiPrediction reply(String content) {
        return new AiPrediction(content, List.of());
    }
}