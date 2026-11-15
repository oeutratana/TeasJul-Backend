package com.example.spring_boot_project_api.service.assistant;

import com.example.spring_boot_project_api.dto.response.AssistantChatResponse;

/**
 * Callback interface for streaming assistant responses. All methods are called
 * sequentially on the worker thread; implementations must be thread-safe with
 * respect to the {@code SseEmitter} they write to.
 *
 * <p>Contract:
 * <ul>
 *   <li>{@link #onDelta} may be called zero or more times before {@link #onDone}.</li>
 *   <li>{@link #onToolEvent} may be called zero or more times (once per tool round).</li>
 *   <li>Exactly one of {@link #onDone} or {@link #onError} is called as the final event.</li>
 * </ul>
 */
public interface AssistantStreamListener {

    /**
     * A text fragment has arrived from the model. May be called many times
     * in rapid succession as the model streams its reply.
     */
    void onDelta(String textFragment);

    /**
     * A tool is about to be executed. The {@code label} is a short human-readable
     * status message suitable for display in the UI (e.g. "Gathering data...").
     */
    void onToolEvent(String label);

    /**
     * The full response is complete. {@code response} is the authoritative final
     * reply (same structure as the non-streaming endpoint).
     */
    void onDone(AssistantChatResponse response);

    /**
     * A fatal error occurred during streaming. After this call no further events
     * will be emitted.
     */
    void onError(String message);
}
