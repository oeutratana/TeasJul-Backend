package com.example.spring_boot_project_api.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.example.spring_boot_project_api.config.AiChatClient;
import com.example.spring_boot_project_api.config.AiProperties;
import com.example.spring_boot_project_api.dto.request.AssistantChatRequest;
import com.example.spring_boot_project_api.dto.request.AssistantHistoryMessage;
import com.example.spring_boot_project_api.dto.response.AssistantChatResponse;
import com.example.spring_boot_project_api.exception.AssistantDisabledException;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.service.AssistantService;
import com.example.spring_boot_project_api.service.assistant.AssistantAuthorization;
import com.example.spring_boot_project_api.service.assistant.AssistantStreamListener;
import com.example.spring_boot_project_api.service.assistant.AssistantTool;
import com.example.spring_boot_project_api.service.assistant.AssistantToolContext;
import com.example.spring_boot_project_api.service.assistant.AssistantToolExecutor;
import com.example.spring_boot_project_api.service.assistant.AiToolResult;
import com.example.spring_boot_project_api.service.assistant.dto.AiChatMessage;
import com.example.spring_boot_project_api.service.assistant.dto.AiPrediction;
import com.example.spring_boot_project_api.service.assistant.dto.AiToolCall;
import com.example.spring_boot_project_api.service.assistant.dto.AiToolDefinition;
import com.example.spring_boot_project_api.util.SecurityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.RequiredArgsConstructor;

/**
 * Orchestrates the assistant conversation on the authenticated request thread:
 * the provider may propose any tool, but only tools allowed for this user's role
 * union are actually executed. All data access happens inside the individual
 * tools via existing services, under the original {@code SecurityContext}.
 */
@Service
@RequiredArgsConstructor
public class AssistantServiceImpl implements AssistantService {

    private static final String FALLBACK_REPLY = "I could not complete your request within the step limit.";

    private static final ObjectMapper JSON = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    private final AiProperties aiProperties;
    private final AiChatClient aiChatClient;
    private final AssistantToolExecutor toolExecutor;
    private final AssistantAuthorization authorization;
    private final SecurityUtils securityUtils;

    @Override
    public AssistantChatResponse chat(AssistantChatRequest request) {
        if (!aiProperties.isEnabled()) {
            throw new AssistantDisabledException();
        }

        User user = securityUtils.getCurrentUser();
        Set<String> roleCodes = authorization.roleCodesOf(user.getId());
        List<AssistantTool> tools = toolExecutor.allTools();
        Set<String> permitted = authorization.permittedToolNames(tools, roleCodes);
        List<AiToolDefinition> definitions = tools.stream()
                .map(AssistantTool::definition)
                .toList();
        AssistantToolContext context = new AssistantToolContext(user.getId(), roleCodes);

        List<AiChatMessage> messages = new ArrayList<>(fromHistory(request));
        messages.add(AiChatMessage.user(request.message()));

        List<AiToolCall> executedCalls = new ArrayList<>();
        boolean anyFailure = false;

        int budget = aiProperties.getMaxToolCalls();
        for (int i = 0; i < budget; i++) {
            AiPrediction prediction =
                    aiChatClient.chat(messages, definitions, List.copyOf(roleCodes));

            if (prediction.toolCalls() == null || prediction.toolCalls().isEmpty()) {
                return new AssistantChatResponse(
                        resolveReply(prediction.content()), executedCalls, anyFailure);
            }

            for (AiToolCall call : prediction.toolCalls()) {
                executedCalls.add(call);
                messages.add(AiChatMessage.assistantCall(List.of(call)));
                if (!permitted.contains(call.name())) {
                    anyFailure = true;
                    messages.add(AiChatMessage.denied(call.name()));
                    continue;
                }
                AiToolResult result = toolExecutor.execute(call.name(), context, call.arguments());
                if (!result.success()) {
                    anyFailure = true;
                }
                messages.add(AiChatMessage.toolResult(format(result), call.name()));
            }
        }

        return new AssistantChatResponse(FALLBACK_REPLY, executedCalls, true);
    }

    @Override
    public void chatStream(AssistantChatRequest request, AssistantStreamListener listener) {
        // isEnabled() is checked by the controller on the request thread before
        // this method is called on the worker thread, but guard again defensively.
        if (!aiProperties.isEnabled()) {
            listener.onError("The AI assistant is currently disabled.");
            return;
        }

        try {
            User user = securityUtils.getCurrentUser();
            Set<String> roleCodes = authorization.roleCodesOf(user.getId());
            List<AssistantTool> tools = toolExecutor.allTools();
            Set<String> permitted = authorization.permittedToolNames(tools, roleCodes);
            List<AiToolDefinition> definitions = tools.stream()
                    .map(AssistantTool::definition)
                    .toList();
            AssistantToolContext context = new AssistantToolContext(user.getId(), roleCodes);

            List<AiChatMessage> messages = new ArrayList<>(fromHistory(request));
            messages.add(AiChatMessage.user(request.message()));

            List<AiToolCall> executedCalls = new ArrayList<>();
            boolean anyFailure = false;
            // Accumulate all streamed text so we can surface it in the final response.
            StringBuilder fullReply = new StringBuilder();

            int budget = aiProperties.getMaxToolCalls();
            for (int i = 0; i < budget; i++) {
                AiPrediction prediction = aiChatClient.chatStream(
                        messages, definitions, List.copyOf(roleCodes),
                        fragment -> {
                            fullReply.append(fragment);
                            listener.onDelta(fragment);
                        });

                if (prediction.toolCalls() == null || prediction.toolCalls().isEmpty()) {
                    // Final text reply — use content from the prediction if it wasn't
                    // streamed (e.g. default Ollama fallback), else the accumulated text.
                    String finalReply = prediction.content() != null && !prediction.content().isBlank()
                            ? prediction.content()
                            : fullReply.toString();
                    listener.onDone(new AssistantChatResponse(
                            resolveReply(finalReply), executedCalls, anyFailure));
                    return;
                }

                // There are tool calls — notify the UI before executing
                listener.onToolEvent("Gathering data...");

                for (AiToolCall call : prediction.toolCalls()) {
                    executedCalls.add(call);
                    messages.add(AiChatMessage.assistantCall(List.of(call)));
                    if (!permitted.contains(call.name())) {
                        anyFailure = true;
                        messages.add(AiChatMessage.denied(call.name()));
                        continue;
                    }
                    AiToolResult result = toolExecutor.execute(call.name(), context, call.arguments());
                    if (!result.success()) {
                        anyFailure = true;
                    }
                    messages.add(AiChatMessage.toolResult(format(result), call.name()));
                }

                // Reset accumulated text for the next model turn (tool result summary)
                fullReply.setLength(0);
            }

            listener.onDone(new AssistantChatResponse(FALLBACK_REPLY, executedCalls, true));

        } catch (AssistantDisabledException ex) {
            listener.onError("The AI assistant is currently disabled.");
        } catch (Exception ex) {
            listener.onError("The AI assistant ran into a problem. Please try again later.");
        }
    }

    private List<AiChatMessage> fromHistory(AssistantChatRequest request) {
        List<AssistantHistoryMessage> history = request.history();
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        List<AssistantHistoryMessage> bounded = history;
        int maxHistory = Math.max(aiProperties.getHistoryMessages(), 1);
        if (bounded.size() > maxHistory) {
            bounded = new ArrayList<>(bounded.subList(bounded.size() - maxHistory, bounded.size()));
        }
        List<AiChatMessage> messages = new ArrayList<>(bounded.size());
        for (AssistantHistoryMessage entry : bounded) {
            messages.add(AiChatMessage.fromHistory(entry.role(), entry.content()));
        }
        return messages;
    }

    private String resolveReply(String content) {
        return (content == null || content.isBlank()) ? "I received your message." : content;
    }

    private String format(AiToolResult result) {
        if (!result.success()) {
            return result.error();
        }
        try {
            return "Tool " + result.toolName() + " returned: "
                    + JSON.writeValueAsString(result.data());
        } catch (JsonProcessingException ex) {
            return "Tool " + result.toolName() + " returned: " + String.valueOf(result.data());
        }
    }
}