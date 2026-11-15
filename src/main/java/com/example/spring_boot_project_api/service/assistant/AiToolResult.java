package com.example.spring_boot_project_api.service.assistant;

/** Result of executing a single tool. */
public record AiToolResult(String toolName, boolean success, Object data, String error) {

    public static AiToolResult success(String toolName, Object data) {
        return new AiToolResult(toolName, true, data, null);
    }

    public static AiToolResult failure(String toolName, String error) {
        return new AiToolResult(toolName, false, null, error);
    }

    public static AiToolResult denied(String toolName) {
        return failure(toolName, AssistantAuthorization.REFUSAL);
    }
}