package com.example.spring_boot_project_api.controller;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.spring_boot_project_api.config.AiProperties;
import com.example.spring_boot_project_api.dto.request.AssistantChatRequest;
import com.example.spring_boot_project_api.dto.response.AssistantChatResponse;
import com.example.spring_boot_project_api.exception.AssistantDisabledException;
import com.example.spring_boot_project_api.service.AssistantService;
import com.example.spring_boot_project_api.service.assistant.AssistantStreamListener;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** AI assistant chat endpoints. Require authentication (default Spring Security rule). */
@Slf4j
@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private static final ObjectMapper JSON = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    private final AssistantService assistantService;
    private final AiProperties aiProperties;
    private final Executor streamExecutor;

    @PostMapping("/chat")
    public ResponseEntity<AssistantChatResponse> chat(
            @Valid @RequestBody AssistantChatRequest request) {
        return ResponseEntity.ok(assistantService.chat(request));
    }

    /**
     * Streaming chat endpoint. Returns a {@code text/event-stream} response where
     * the assistant reply is delivered as a series of SSE events:
     * <ul>
     *   <li>{@code event: delta} — a text fragment from the model</li>
     *   <li>{@code event: tool}  — a tool is being executed ("Gathering data...")</li>
     *   <li>{@code event: done}  — the final {@link AssistantChatResponse} JSON</li>
     *   <li>{@code event: error} — an error message string; stream ends</li>
     * </ul>
     *
     * <p>The {@code AssistantDisabledException} check is intentionally performed on
     * the request thread so the standard {@code GlobalExceptionHandler} can map it
     * to HTTP 403 before the emitter is created.</p>
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody AssistantChatRequest request) {
        if (!aiProperties.isEnabled()) {
            throw new AssistantDisabledException();
        }

        long timeoutMs = (long) aiProperties.getReadTimeoutSeconds() * 1000L;
        SseEmitter emitter = new SseEmitter(timeoutMs);

        // Capture the SecurityContext on the request thread so the worker thread
        // can resolve the authenticated user via SecurityContextHolder.
        Runnable work = DelegatingSecurityContextRunnable.create(
                () -> assistantService.chatStream(request, listenerFor(emitter)),
                SecurityContextHolder.getContext());

        emitter.onTimeout(emitter::complete);
        emitter.onError(t -> emitter.complete());

        streamExecutor.execute(work);
        return emitter;
    }

    /** Builds a listener that translates service events into SSE frames on {@code emitter}. */
    private AssistantStreamListener listenerFor(SseEmitter emitter) {
        return new AssistantStreamListener() {
            @Override
            public void onDelta(String textFragment) {
                sendEvent(emitter, "delta", textFragment);
            }

            @Override
            public void onToolEvent(String label) {
                sendEvent(emitter, "tool", label);
            }

            @Override
            public void onDone(AssistantChatResponse response) {
                try {
                    sendEvent(emitter, "done", JSON.writeValueAsString(response));
                } catch (JsonProcessingException ex) {
                    log.warn("Failed to serialize final stream response", ex);
                    sendEvent(emitter, "done", "{\"reply\":\"Done.\",\"error\":false,\"toolCalls\":[]}");
                }
                emitter.complete();
            }

            @Override
            public void onError(String message) {
                sendEvent(emitter, "error", message);
                emitter.complete();
            }
        };
    }

    private static void sendEvent(SseEmitter emitter, String eventName, String data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (Exception ex) {
            // Client disconnected — ignore; emitter will time out or be completed
            log.debug("SSE send failed (client likely disconnected) event={}: {}", eventName, ex.getMessage());
        }
    }

    /** Dedicated executor for SSE worker threads. Package-private for testability. */
    @Bean
    static Executor streamExecutor() {
        return Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "assistant-stream-worker");
            t.setDaemon(true);
            return t;
        });
    }
}