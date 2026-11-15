package com.example.spring_boot_project_api.service.assistant.dto;

import java.util.Collections;
import java.util.Map;

public record AiToolCall(String name, Map<String, String> arguments) {

    public AiToolCall {
        arguments = arguments == null ? Collections.emptyMap() : Map.copyOf(arguments);
    }
}