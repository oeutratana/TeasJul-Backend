package com.example.spring_boot_project_api.service.assistant.dto;

import java.util.Collections;
import java.util.Map;

/** Provider-facing description of a tool the assistant may suggest using. */
public record AiToolDefinition(String name, String description, Map<String, String> parameters) {

    public AiToolDefinition {
        parameters = parameters == null ? Collections.emptyMap() : Map.copyOf(parameters);
    }
}