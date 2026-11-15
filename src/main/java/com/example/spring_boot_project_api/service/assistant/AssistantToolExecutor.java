package com.example.spring_boot_project_api.service.assistant;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Registry and executor of every {@link AssistantTool}. It performs a second,
 * independent role check before any tool runs (defense in depth) and converts
 * any failure — including access denials thrown by the existing services — into
 * a normal {@link AiToolResult} so the mock can formulate a reply without
 * leaking stack traces.
 */
@Component
@RequiredArgsConstructor
public class AssistantToolExecutor {

    private final List<AssistantTool> tools;

    public List<AssistantTool> allTools() {
        return tools;
    }

    public Map<String, AssistantTool> toolsByName() {
        return tools.stream()
                .collect(Collectors.toMap(AssistantTool::name, Function.identity()));
    }

    public AiToolResult execute(String toolName, AssistantToolContext context,
            Map<String, String> rawArgs) {
        AssistantTool tool = toolsByName().get(toolName);
        if (tool == null) {
            return AiToolResult.failure(toolName, "Unknown tool: " + toolName);
        }
        if (!isRoleAllowed(tool, context)) {
            return AiToolResult.denied(toolName);
        }
        try {
            return tool.execute(context, new AssistantArguments(rawArgs));
        } catch (AccessDeniedException ex) {
            return AiToolResult.denied(toolName);
        } catch (RuntimeException ex) {
            String message = ex.getMessage();
            return AiToolResult.failure(toolName,
                    message == null || message.isBlank() ? "The tool failed to run" : message);
        }
    }

    private boolean isRoleAllowed(AssistantTool tool, AssistantToolContext context) {
        for (String role : tool.allowedRoles()) {
            if (context.roleCodes().contains(role)) {
                return true;
            }
        }
        return false;
    }
}