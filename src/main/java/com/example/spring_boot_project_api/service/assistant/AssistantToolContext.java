package com.example.spring_boot_project_api.service.assistant;

import java.util.Map;
import java.util.Set;

/** Identity of the authenticated user a tool call is running as. */
public record AssistantToolContext(Long userId, Set<String> roleCodes) {

    public AssistantToolContext {
        roleCodes = roleCodes == null ? Set.of() : Set.copyOf(roleCodes);
    }
}