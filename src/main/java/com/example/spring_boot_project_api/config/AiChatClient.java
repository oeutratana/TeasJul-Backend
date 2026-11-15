package com.example.spring_boot_project_api.config;

import java.util.List;
import java.util.function.Consumer;

import com.example.spring_boot_project_api.service.assistant.dto.AiChatMessage;
import com.example.spring_boot_project_api.service.assistant.dto.AiPrediction;
import com.example.spring_boot_project_api.service.assistant.dto.AiToolDefinition;

/**
 * Abstraction over the LLM backend. The implementation decides, given the
 * conversation so far, a description of the tools it may suggest and the roles
 * of the authenticated user, what to reply and/or which tool call to propose.
 *
 * <p>Role-aware permission filtering is intentionally done by the assistant
 * service layer, never by the provider: the provider may propose any tool and
 * the backend decides whether the authenticated user is allowed to run it.</p>
 */
public interface AiChatClient {

    AiPrediction chat(List<AiChatMessage> messages, List<AiToolDefinition> tools,
            List<String> roleCodes);

    /**
     * Streaming variant. Fires zero or more {@code textDelta} callbacks as the
     * model replies, then returns the final {@link AiPrediction} (which may
     * carry tool-call requests instead of content).
     *
     * <p>The default implementation delegates to {@link #chat} and fires the
     * complete reply as a single delta — providers that do not override this
     * method still satisfy the streaming contract with a single chunk.</p>
     */
    default AiPrediction chatStream(List<AiChatMessage> messages, List<AiToolDefinition> tools,
            List<String> roleCodes, Consumer<String> textDelta) {
        AiPrediction result = chat(messages, tools, roleCodes);
        if (result.content() != null && !result.content().isBlank()) {
            textDelta.accept(result.content());
        }
        return result;
    }
}