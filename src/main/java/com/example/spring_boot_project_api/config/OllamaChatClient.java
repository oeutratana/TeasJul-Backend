package com.example.spring_boot_project_api.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.example.spring_boot_project_api.service.assistant.dto.AiChatMessage;
import com.example.spring_boot_project_api.service.assistant.dto.AiPrediction;
import com.example.spring_boot_project_api.service.assistant.dto.AiToolCall;
import com.example.spring_boot_project_api.service.assistant.dto.AiToolDefinition;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Real LLM provider backed by a local Ollama server (native {@code /api/chat}).
 * Active only when {@code app.ai.provider=ollama}; {@link MockChatClient}
 * remains the default. Like the mock, this client merely "understands" the
 * conversation and proposes tools — all authorization and data access still
 * happen in the assistant service layer and tool executor.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "ollama")
@RequiredArgsConstructor
public class OllamaChatClient implements AiChatClient {

    private static final String UNCONFIGURED_MODEL = "mock";
    private static final String REQUIRED_MARKER = "(required)";
    private static final String FALLBACK_REPLY = "I received your message.";

    private static final ObjectMapper JSON = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    private final AiProperties aiProperties;

    private RestTemplate restTemplate;

    @Override
    public AiPrediction chat(List<AiChatMessage> messages, List<AiToolDefinition> tools,
            List<String> roleCodes) {
        String model = aiProperties.getModel();
        if (model == null || model.isBlank() || UNCONFIGURED_MODEL.equals(model)) {
            return AiPrediction.reply("The AI assistant is not configured. "
                    + "Set the AI_MODEL environment variable to an Ollama model "
                    + "(for example qwen3:4b) before enabling the ollama provider.");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("messages", toOllamaMessages(messages));
        if (tools != null && !tools.isEmpty()) {
            payload.put("tools", tools.stream().map(this::toOllamaTool).toList());
        }
        payload.put("stream", false);

        String endpoint = aiProperties.getBaseUrl() + "/api/chat";
        int messageCount = messages.size();
        int toolCount = tools == null ? 0 : tools.size();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(JSON.writeValueAsString(payload), headers);

            long start = System.currentTimeMillis();
            ResponseEntity<String> response = restTemplate().exchange(
                    endpoint, HttpMethod.POST, request, String.class);
            long elapsed = System.currentTimeMillis() - start;

            log.info("Ollama model={} messages={} tools={} status={} duration={}ms",
                    model, messageCount, toolCount,
                    response.getStatusCode().value(), elapsed);

            String body = response.getBody();
            if (body == null || body.isBlank()) {
                return AiPrediction.reply("The assistant returned an empty response.");
            }
            return parse(body);
        } catch (HttpStatusCodeException ex) {
            log.warn("Ollama chat failed with HTTP {} model={} messages={} tools={}: {}",
                    ex.getStatusCode(), model, messageCount, toolCount,
                    ex.getResponseBodyAsString());
            return AiPrediction.reply("The local AI assistant is unavailable right now. "
                    + "Please try again later.");
        } catch (ResourceAccessException ex) {
            log.warn("Ollama connection failed model={} endpoint={}: {}",
                    model, endpoint, ex.getMessage());
            return AiPrediction.reply("I could not reach the local AI assistant. "
                    + "Please make sure Ollama is running and try again later.");
        } catch (RestClientException ex) {
            log.warn("Ollama chat failed model={}: {}", model, ex.getMessage());
            return AiPrediction.reply("The local AI assistant ran into a problem. "
                    + "Please try again later.");
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            log.warn("Ollama response could not be parsed model={}: {}", model, ex.getMessage());
            return AiPrediction.reply("I received an unreadable response from the "
                    + "assistant backend.");
        }
    }

    private AiPrediction parse(String responseBody)
            throws JsonProcessingException {
        JsonNode root = JSON.readTree(responseBody);
        JsonNode message = root.path("message");
        if (message.isMissingNode() || message.isNull()) {
            throw new IllegalArgumentException("response has no message object");
        }

        String content = message.path("content").isValueNode()
                ? message.path("content").asText()
                : null;

        List<AiToolCall> calls = parseToolCalls(message.path("tool_calls"));
        if (!calls.isEmpty()) {
            return new AiPrediction(content, calls);
        }

        // Some fine-tuned local models (e.g. qwen2.5-coder) do not emit the
        // native tool_calls array; instead they write the tool call as JSON
        // text in the content field. Detect that shape so the tool is still
        // routed through the (authorization-checked) executor.
        List<AiToolCall> contentCalls = parseContentToolCalls(content);
        if (!contentCalls.isEmpty()) {
            return new AiPrediction(null, contentCalls);
        }

        return AiPrediction.reply(
                content == null || content.isBlank() ? FALLBACK_REPLY : content);
    }

    /**
     * Best-effort fallback for models that surface their tool call as raw JSON
     * text (e.g. {@code {"name":"get_house_summary","arguments":{...}}}) in the
     * assistant content instead of the Ollama {@code tool_calls} array. Only a
     * strict JSON object with a non-blank {@code name} is accepted; anything
     * else is left as a regular reply.
     */
    private List<AiToolCall> parseContentToolCalls(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        String candidate = stripCodeFences(content.trim());
        if (!candidate.startsWith("{")) {
            return List.of();
        }
        try {
            JsonNode node = JSON.readTree(candidate);
            if (!node.isObject()) {
                return List.of();
            }
            JsonNode name = node.path("name");
            if (!name.isTextual() || name.asText().isBlank()) {
                return List.of();
            }
            JsonNode args = node.path("arguments");
            Map<String, String> argumentMap = new LinkedHashMap<>();
            if (args.isObject()) {
                stringifyInto(args, argumentMap);
            } else if (args.isTextual() && !args.asText().isBlank()) {
                JsonNode parsed = JSON.readTree(args.asText());
                if (parsed.isObject()) {
                    stringifyInto(parsed, argumentMap);
                }
            }
            return List.of(new AiToolCall(name.asText(), argumentMap));
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            return List.of();
        }
    }

    private static String stripCodeFences(String text) {
        if (text.startsWith("```")) {
            int firstLineEnd = text.indexOf('\n');
            if (firstLineEnd != -1) {
                String body = text.substring(firstLineEnd + 1);
                if (body.endsWith("```")) {
                    body = body.substring(0, body.length() - 3);
                }
                return body.trim();
            }
        }
        return text;
    }

    private List<AiToolCall> parseToolCalls(JsonNode toolCalls) {
        List<AiToolCall> calls = new ArrayList<>();
        if (toolCalls == null || !toolCalls.isArray()) {
            return calls;
        }
        for (JsonNode entry : toolCalls) {
            JsonNode function = entry.path("function");
            String name = function.path("name").asText();
            if (name == null || name.isBlank()) {
                continue;
            }
            Map<String, String> arguments = new LinkedHashMap<>();
            stringifyInto(function.path("arguments"), arguments);
            calls.add(new AiToolCall(name, arguments));
        }
        return calls;
    }

    private void stringifyInto(JsonNode arguments, Map<String, String> result) {
        if (arguments == null || !arguments.isObject()) {
            return;
        }
        arguments.properties().forEach(entry -> {
            JsonNode value = entry.getValue();
            if (value != null && !value.isNull()) {
                result.put(entry.getKey(), value.asText());
            }
        });
    }

    private List<Map<String, Object>> toOllamaMessages(List<AiChatMessage> messages) {
        List<Map<String, Object>> result = new ArrayList<>(messages.size());
        for (AiChatMessage message : messages) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("role", message.role());
            map.put("content", message.content() == null ? "" : message.content());
            if (message.toolName() != null) {
                map.put("tool_name", message.toolName());
            }
            List<AiToolCall> calls = message.toolCalls();
            if (calls != null && !calls.isEmpty()) {
                map.put("tool_calls", calls.stream()
                        .map(this::toOllamaToolCall)
                        .toList());
            }
            result.add(map);
        }
        return result;
    }

    private Map<String, Object> toOllamaToolCall(AiToolCall call) {
        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", call.name());
        function.put("arguments", call.arguments());
        return Map.of("function", function);
    }

    private Map<String, Object> toOllamaTool(AiToolDefinition definition) {
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        definition.parameters().forEach((name, description) -> {
            Map<String, Object> property = new LinkedHashMap<>();
            property.put("type", "string");
            property.put("description", description);
            properties.put(name, property);
            if (description != null && description.contains(REQUIRED_MARKER)) {
                required.add(name);
            }
        });

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("type", "object");
        parameters.put("properties", properties);
        if (!required.isEmpty()) {
            parameters.put("required", required);
        }

        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", definition.name());
        function.put("description", definition.description());
        function.put("parameters", parameters);

        return Map.of("type", "function", "function", function);
    }

    /** Lazily-created client, package-private so tests can bind a mock server to it. */
    RestTemplate restTemplate() {
        RestTemplate local = restTemplate;
        if (local == null) {
            local = createRestTemplate(aiProperties.getReadTimeoutSeconds());
            restTemplate = local;
        }
        return local;
    }

    private static RestTemplate createRestTemplate(int readTimeoutSeconds) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(Math.max(readTimeoutSeconds, 1)));
        return new RestTemplate(factory);
    }
}