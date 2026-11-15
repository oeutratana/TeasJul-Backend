package com.example.spring_boot_project_api.service.assistant.dto;

import java.util.List;

import com.example.spring_boot_project_api.service.assistant.AssistantAuthorization;

/**
 * One conversation message exchanged with the LLM provider. Roles are
 * {@code user}/{@code assistant} for turns coming from the chat request and
 * {@code tool} for tool results that have already been authorization-checked.
 * An {@code assistant} message that proposed a tool may also carry the
 * {@code toolCalls} it made so providers that need an explicit
 * {@code assistant(tool_calls)} message before each {@code tool} result (such
 * as Ollama) can reconstruct the request exactly.
 */
public record AiChatMessage(String role, String content, String toolName,
        List<AiToolCall> toolCalls) {

    public AiChatMessage(String role, String content, String toolName) {
        this(role, content, toolName, null);
    }

    public AiChatMessage {
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }

    public static AiChatMessage user(String content) {
        return new AiChatMessage("user", content, null, null);
    }

    public static AiChatMessage assistant(String content) {
        return new AiChatMessage("assistant", content, null, null);
    }

    /**
     * Role preserved as-is from the client ({@code user} or {@code assistant})
     * so the provider can keep reasoning across turns.
     */
    public static AiChatMessage fromHistory(String role, String content) {
        return new AiChatMessage(role, content, null, null);
    }

    /** Assistant turn that issued one or more tool calls, before any tool result. */
    public static AiChatMessage assistantCall(List<AiToolCall> calls) {
        return new AiChatMessage("assistant", null, null, calls);
    }

    public static AiChatMessage toolResult(String content, String toolName) {
        return new AiChatMessage("tool", content, toolName, null);
    }

    public static AiChatMessage denied(String toolName) {
        return new AiChatMessage("tool", AssistantAuthorization.REFUSAL, toolName, null);
    }
}