package com.example.spring_boot_project_api.config;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

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
 * LLM provider backed by the OpenRouter API (OpenAI-compatible Chat Completions).
 * Active only when {@code app.ai.provider=openrouter}.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "openrouter")
@RequiredArgsConstructor
public class OpenRouterChatClient implements AiChatClient {

    private static final String UNCONFIGURED_MODEL = "";
    private static final String REQUIRED_MARKER = "(required)";
    private static final String FALLBACK_REPLY = "I received your message.";

    private static final ObjectMapper JSON = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    private final AiProperties aiProperties;

    private RestTemplate restTemplate;
    private RestTemplate streamRestTemplate;

    @Override
    public AiPrediction chat(List<AiChatMessage> messages, List<AiToolDefinition> tools,
            List<String> roleCodes) {
        String model = aiProperties.getModel();
        if (model == null || model.isBlank() || UNCONFIGURED_MODEL.equals(model)) {
            return AiPrediction.reply("The AI assistant is not configured. "
                    + "Set the OPENROUTER_MODEL environment variable to a valid model ID "
                    + "(for example anthropic/claude-3.5-sonnet) before enabling the openrouter provider.");
        }

        String apiKey = aiProperties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return AiPrediction.reply("The AI assistant is not configured. "
                    + "Set the OPENROUTER_API_KEY environment variable before enabling the openrouter provider.");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("messages", toOpenAiMessages(messages));
        if (tools != null && !tools.isEmpty()) {
            payload.put("tools", tools.stream().map(this::toOpenAiTool).toList());
        }

        String endpoint = aiProperties.getBaseUrl() + "/chat/completions";
        int messageCount = messages.size();
        int toolCount = tools == null ? 0 : tools.size();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            headers.set("HTTP-Referer", "https://teasjul.com");
            headers.set("X-Title", "TeasJul AI Assistant");
            HttpEntity<String> request = new HttpEntity<>(JSON.writeValueAsString(payload), headers);

            long start = System.currentTimeMillis();
            ResponseEntity<String> response = restTemplate().exchange(
                    endpoint, HttpMethod.POST, request, String.class);
            long elapsed = System.currentTimeMillis() - start;

            log.info("OpenRouter model={} messages={} tools={} status={} duration={}ms",
                    model, messageCount, toolCount,
                    response.getStatusCode().value(), elapsed);

            String body = response.getBody();
            if (body == null || body.isBlank()) {
                return AiPrediction.reply("The assistant returned an empty response.");
            }
            return parse(body);
        } catch (HttpStatusCodeException ex) {
            int status = ex.getStatusCode().value();
            log.warn("OpenRouter chat failed with HTTP {} model={} messages={} tools={}: {}",
                    status, model, messageCount, toolCount,
                    ex.getResponseBodyAsString());
            return AiPrediction.reply(handleHttpError(status));
        } catch (ResourceAccessException ex) {
            log.warn("OpenRouter connection failed model={} endpoint={}: {}",
                    model, endpoint, ex.getMessage());
            return AiPrediction.reply("Could not reach the AI assistant. "
                    + "Please check your network connection and try again later.");
        } catch (RestClientException ex) {
            log.warn("OpenRouter chat failed model={}: {}", model, ex.getMessage());
            return AiPrediction.reply("The AI assistant ran into a problem. "
                    + "Please try again later.");
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            log.warn("OpenRouter response could not be parsed model={}: {}", model, ex.getMessage());
            return AiPrediction.reply("I received an unreadable response from the "
                    + "assistant backend.");
        }
    }

    @Override
    public AiPrediction chatStream(List<AiChatMessage> messages, List<AiToolDefinition> tools,
            List<String> roleCodes, Consumer<String> textDelta) {
        String model = aiProperties.getModel();
        if (model == null || model.isBlank() || UNCONFIGURED_MODEL.equals(model)) {
            return AiPrediction.reply("The AI assistant is not configured. "
                    + "Set the OPENROUTER_MODEL environment variable to a valid model ID "
                    + "(for example anthropic/claude-3.5-sonnet) before enabling the openrouter provider.");
        }

        String apiKey = aiProperties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return AiPrediction.reply("The AI assistant is not configured. "
                    + "Set the OPENROUTER_API_KEY environment variable before enabling the openrouter provider.");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("messages", toOpenAiMessages(messages));
        payload.put("stream", true);
        if (tools != null && !tools.isEmpty()) {
            payload.put("tools", tools.stream().map(this::toOpenAiTool).toList());
        }

        String endpoint = aiProperties.getBaseUrl() + "/chat/completions";
        int messageCount = messages.size();
        int toolCount = tools == null ? 0 : tools.size();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            headers.set("HTTP-Referer", "https://teasjul.com");
            headers.set("X-Title", "TeasJul AI Assistant");
            HttpEntity<String> request = new HttpEntity<>(JSON.writeValueAsString(payload), headers);

            long start = System.currentTimeMillis();

            // Accumulate text content and tool_call fragments (by index)
            StringBuilder contentBuilder = new StringBuilder();
            // index -> {name, arguments}
            Map<Integer, Map<String, StringBuilder>> toolFragments = new TreeMap<>();

            streamRestTemplate().execute(endpoint, HttpMethod.POST, req -> {
                req.getHeaders().addAll(headers);
                byte[] body = JSON.writeValueAsBytes(payload);
                req.getBody().write(body);
            }, response -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.startsWith("data:")) continue;
                        String data = line.substring(5).trim();
                        if ("[DONE]".equals(data)) break;
                        if (data.isEmpty()) continue;
                        try {
                            JsonNode frame = JSON.readTree(data);

                            // Sometimes OpenRouter streams an error object instead of choices
                            JsonNode errorNode = frame.path("error");
                            if (errorNode.isObject()) {
                                String errorMsg = errorNode.path("message").asText("Unknown provider error");
                                contentBuilder.append(errorMsg);
                                textDelta.accept(errorMsg);
                                continue;
                            }

                            JsonNode choices = frame.path("choices");
                            if (!choices.isArray() || choices.isEmpty()) continue;
                            JsonNode delta = choices.get(0).path("delta");

                            // Text content delta
                            JsonNode contentNode = delta.path("content");
                            if (contentNode.isTextual() && !contentNode.asText().isEmpty()) {
                                String fragment = contentNode.asText();
                                contentBuilder.append(fragment);
                                textDelta.accept(fragment);
                            }

                            // Tool call fragments — accumulate by index
                            JsonNode toolCallsNode = delta.path("tool_calls");
                            if (toolCallsNode.isArray()) {
                                for (JsonNode tc : toolCallsNode) {
                                    int idx = tc.path("index").asInt(0);
                                    toolFragments.computeIfAbsent(idx, i -> {
                                        Map<String, StringBuilder> m = new LinkedHashMap<>();
                                        m.put("name", new StringBuilder());
                                        m.put("arguments", new StringBuilder());
                                        return m;
                                    });
                                    JsonNode fn = tc.path("function");
                                    JsonNode nameNode = fn.path("name");
                                    if (nameNode.isTextual()) {
                                        toolFragments.get(idx).get("name").append(nameNode.asText());
                                    }
                                    JsonNode argsNode = fn.path("arguments");
                                    if (argsNode.isTextual()) {
                                        toolFragments.get(idx).get("arguments").append(argsNode.asText());
                                    }
                                }
                            }
                        } catch (JsonProcessingException ex) {
                            log.debug("Skipping unparseable SSE frame: {}", data);
                        }
                    }
                }
                return null;
            });

            long elapsed = System.currentTimeMillis() - start;
            log.info("OpenRouter stream model={} messages={} tools={} duration={}ms",
                    model, messageCount, toolCount, elapsed);

            // Assemble tool calls from accumulated fragments
            List<AiToolCall> calls = assembleToolCalls(toolFragments);
            if (!calls.isEmpty()) {
                return new AiPrediction(contentBuilder.isEmpty() ? null : contentBuilder.toString(), calls);
            }

            // Fall back to content or content-embedded tool calls
            String fullContent = contentBuilder.toString();
            List<AiToolCall> contentCalls = parseContentToolCalls(fullContent);
            if (!contentCalls.isEmpty()) {
                return new AiPrediction(null, contentCalls);
            }

            return AiPrediction.reply(fullContent.isBlank() ? FALLBACK_REPLY : fullContent);

        } catch (HttpStatusCodeException ex) {
            int status = ex.getStatusCode().value();
            log.warn("OpenRouter stream failed with HTTP {} model={}: {}",
                    status, model, ex.getResponseBodyAsString());
            return AiPrediction.reply(handleHttpError(status));
        } catch (ResourceAccessException ex) {
            log.warn("OpenRouter stream connection failed model={} endpoint={}: {}",
                    model, endpoint, ex.getMessage());
            return AiPrediction.reply("Could not reach the AI assistant. "
                    + "Please check your network connection and try again later.");
        } catch (RestClientException ex) {
            log.warn("OpenRouter stream failed model={}: {}", model, ex.getMessage());
            return AiPrediction.reply("The AI assistant ran into a problem. "
                    + "Please try again later.");
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            log.warn("OpenRouter stream payload serialization failed model={}: {}", model, ex.getMessage());
            return AiPrediction.reply("I received an unreadable response from the assistant backend.");
        }
    }

    /**
     * Assembles complete {@link AiToolCall}s from the per-index fragment buffers
     * accumulated during SSE streaming.
     */
    private List<AiToolCall> assembleToolCalls(
            Map<Integer, Map<String, StringBuilder>> toolFragments) {
        List<AiToolCall> calls = new ArrayList<>();
        for (Map<String, StringBuilder> fragment : toolFragments.values()) {
            String name = fragment.get("name").toString().trim();
            if (name.isBlank()) continue;
            String argsJson = fragment.get("arguments").toString();
            Map<String, String> arguments = new LinkedHashMap<>();
            if (!argsJson.isBlank()) {
                try {
                    JsonNode argsNode = JSON.readTree(argsJson);
                    collectArguments(argsNode, arguments);
                } catch (JsonProcessingException ex) {
                    log.debug("Could not parse streamed tool arguments for {}: {}", name, argsJson);
                }
            }
            calls.add(new AiToolCall(name, arguments));
        }
        return calls;
    }

    private String handleHttpError(int status) {
        return switch (status) {
            case 401 -> "Invalid API key. Please check the OPENROUTER_API_KEY configuration.";
            case 403 -> "Access denied by OpenRouter. Your API key may lack the required permissions.";
            case 429 -> "Too many requests to the AI assistant. Please wait a moment and try again.";
            case 500, 502, 503 -> "The AI assistant service is temporarily unavailable. "
                    + "Please try again later.";
            default -> "The AI assistant is unavailable right now (HTTP " + status + "). "
                    + "Please try again later.";
        };
    }

    private AiPrediction parse(String responseBody)
            throws JsonProcessingException {
        JsonNode root = JSON.readTree(responseBody);
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new IllegalArgumentException("response has no choices array");
        }

        JsonNode first = choices.get(0);
        JsonNode message = first.path("message");
        if (message.isMissingNode() || message.isNull()) {
            throw new IllegalArgumentException("response choice has no message");
        }

        String content = message.path("content").isValueNode()
                ? message.path("content").asText()
                : null;

        List<AiToolCall> calls = parseToolCalls(message.path("tool_calls"));
        if (!calls.isEmpty()) {
            return new AiPrediction(content, calls);
        }

        // Some models do not emit the native tool_calls array; instead they write
        // the tool call as JSON text in the content field. Detect that shape so
        // the tool is still routed through the (authorization-checked) executor.
        List<AiToolCall> contentCalls = parseContentToolCalls(content);
        if (!contentCalls.isEmpty()) {
            return new AiPrediction(null, contentCalls);
        }

        return AiPrediction.reply(
                content == null || content.isBlank() ? FALLBACK_REPLY : content);
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
            collectArguments(function.path("arguments"), arguments);
            calls.add(new AiToolCall(name, arguments));
        }
        return calls;
    }

    /**
     * Best-effort fallback for models that surface their tool call as raw JSON
     * text (e.g. {@code {"name":"get_house_summary","arguments":{...}}}) in the
     * assistant content instead of the native {@code tool_calls} array. Only a
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
            collectArguments(args, argumentMap);
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

    /**
     * OpenRouter/OpenAI returns {@code arguments} as a JSON-encoded string
     * (e.g. {@code "{\"houseId\":\"12\"}"}) rather than an object, so parse the
     * string first when needed and collect each entry as plain text.
     */
    private void collectArguments(JsonNode argumentsNode, Map<String, String> result) {
        if (argumentsNode == null || argumentsNode.isNull()) {
            return;
        }
        JsonNode parsed = null;
        if (argumentsNode.isTextual() && !argumentsNode.asText().isBlank()) {
            try {
                parsed = JSON.readTree(argumentsNode.asText());
            } catch (JsonProcessingException ex) {
                parsed = null;
            }
        } else if (argumentsNode.isObject()) {
            parsed = argumentsNode;
        }
        if (parsed != null) {
            stringifyInto(parsed, result);
        }
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

    private List<Map<String, Object>> toOpenAiMessages(List<AiChatMessage> messages) {
        List<Map<String, Object>> result = new ArrayList<>(messages.size());
        for (AiChatMessage message : messages) {
            Map<String, Object> map = new LinkedHashMap<>();
            String role = message.role();
            map.put("role", role);

            if ("tool".equals(role)) {
                map.put("content", message.content() == null ? "" : message.content());
                map.put("tool_call_id", message.toolName());
            } else {
                map.put("content", message.content() == null ? "" : message.content());
                List<AiToolCall> calls = message.toolCalls();
                if (calls != null && !calls.isEmpty()) {
                    map.put("tool_calls", calls.stream()
                            .map(this::toOpenAiToolCall)
                            .toList());
                }
            }
            result.add(map);
        }
        return result;
    }

    private Map<String, Object> toOpenAiToolCall(AiToolCall call) {
        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", call.name());
        function.put("arguments", jsonString(call.arguments()));
        return Map.of("id", "call_" + call.name(), "type", "function", "function", function);
    }

    /** OpenAI expects tool-call arguments as a JSON-encoded string. */
    private String jsonString(Map<String, String> arguments) {
        try {
            return JSON.writeValueAsString(arguments == null ? Map.of() : arguments);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private Map<String, Object> toOpenAiTool(AiToolDefinition definition) {
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

    /**
     * Lazily-created streaming client. Uses a longer read timeout than the
     * non-streaming client because the stream may be open for the full model
     * generation time. Package-private so tests can inject a mock server.
     */
    RestTemplate streamRestTemplate() {
        RestTemplate local = streamRestTemplate;
        if (local == null) {
            // For streaming, use the configured timeout (same as non-streaming).
            // SimpleClientHttpRequestFactory buffers responses by default; we
            // disable response buffering so the InputStream stays live.
            local = createStreamRestTemplate(aiProperties.getReadTimeoutSeconds());
            streamRestTemplate = local;
        }
        return local;
    }

    private static RestTemplate createRestTemplate(int readTimeoutSeconds) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(Math.max(readTimeoutSeconds, 1)));
        return new RestTemplate(factory);
    }

    private static RestTemplate createStreamRestTemplate(int readTimeoutSeconds) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(Math.max(readTimeoutSeconds, 1)));
        return new RestTemplate(factory);
    }
}
