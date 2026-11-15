package com.example.spring_boot_project_api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Configuration for the AI assistant. Supported providers:
 * {@code mock} (deterministic, no external calls),
 * {@code ollama} (local Ollama server),
 * {@code openrouter} (OpenRouter cloud API).
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    /** Master switch. When false the assistant endpoint responds with HTTP 403. */
    private boolean enabled = true;

    /** Provider id: {@code mock}, {@code ollama}, or {@code openrouter}. */
    private String provider = "openrouter";

    /** Model id sent to the provider. */
    private String model = "";

    /** Base URL of the LLM backend. */
    private String baseUrl = "https://openrouter.ai/api/v1";

    /** API key for providers that require authentication (e.g. OpenRouter). */
    private String apiKey = "";

    /** Response read timeout for the LLM backend, in seconds. */
    private int readTimeoutSeconds = 180;

    /** Maximum number of tool-call rounds per user message. */
    private int maxToolCalls = 6;

    /** Maximum number of prior conversation history messages sent to the model. */
    private int historyMessages = 12;
}