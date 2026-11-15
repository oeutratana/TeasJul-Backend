package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.request.AssistantChatRequest;
import com.example.spring_boot_project_api.dto.response.AssistantChatResponse;
import com.example.spring_boot_project_api.service.assistant.AssistantStreamListener;

public interface AssistantService {

    AssistantChatResponse chat(AssistantChatRequest request);

    /**
     * Streaming variant. The {@code listener} is called on the worker thread
     * as events arrive. The method is non-blocking: it submits work and
     * returns immediately. Authorization is checked on the caller's thread
     * before any async work begins.
     *
     * <p>Implementations must guarantee that exactly one of
     * {@link AssistantStreamListener#onDone} or
     * {@link AssistantStreamListener#onError} is called, even under exceptions.</p>
     */
    void chatStream(AssistantChatRequest request, AssistantStreamListener listener);
}