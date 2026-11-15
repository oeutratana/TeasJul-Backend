package com.example.spring_boot_project_api.service.assistant;

import java.util.Map;
import java.util.Set;

import com.example.spring_boot_project_api.service.assistant.dto.AiToolDefinition;

/**
 * A read-only capability the assistant may invoke on behalf of the
 * authenticated user. Implementations must delegate to existing business
 * services and must never touch repositories or write data.
 */
public interface AssistantTool {

    /** Unique snake_case name used on the wire and in tool calls. */
    String name();

    /** Human/LLM-readable description used for routing. */
    String description();

    /** Role codes that may invoke this tool. */
    Set<String> allowedRoles();

    /** Parameter names with a short description; values are wire strings. */
    Map<String, String> parameters();

    AiToolResult execute(AssistantToolContext context, AssistantArguments args);

    default AiToolDefinition definition() {
        return new AiToolDefinition(name(), description(), parameters());
    }
}