package com.example.spring_boot_project_api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import com.example.spring_boot_project_api.service.assistant.dto.AiChatMessage;
import com.example.spring_boot_project_api.service.assistant.dto.AiPrediction;
import com.example.spring_boot_project_api.service.assistant.dto.AiToolCall;
import com.example.spring_boot_project_api.service.assistant.dto.AiToolDefinition;

/**
 * Unit tests for the OpenRouter provider against a mocked HTTP backend; no live
 * API calls are made.
 */
class OpenRouterChatClientTest {

    private AiProperties properties;
    private OpenRouterChatClient client;

    @BeforeEach
    void setUp() {
        properties = new AiProperties();
        properties.setBaseUrl("https://openrouter.ai/api/v1");
        properties.setApiKey("test-api-key-12345");
        properties.setModel("anthropic/claude-3.5-sonnet");
        properties.setReadTimeoutSeconds(180);
        client = new OpenRouterChatClient(properties);
    }

    @Test
    void returnsPlainReplyWhenNoToolCalls() {
        MockRestServiceServer server = MockRestServiceServer.createServer(client.restTemplate());
        server.expect(requestTo("https://openrouter.ai/api/v1/chat/completions"))
                .andExpect(method(POST))
                .andExpect(header("Authorization", "Bearer test-api-key-12345"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess(
                        "{\"choices\":[{\"message\":{\"role\":\"assistant\","
                                + "\"content\":\"Hello there!\"}}]}",
                        MediaType.APPLICATION_JSON));

        AiPrediction prediction =
                client.chat(List.of(AiChatMessage.user("hi")), List.of(), List.of("OWNER"));

        assertThat(prediction.content()).isEqualTo("Hello there!");
        assertThat(prediction.toolCalls()).isEmpty();
    }

    @Test
    void parsesToolCallsFromOpenAiFormat() {
        MockRestServiceServer server = MockRestServiceServer.createServer(client.restTemplate());
        server.expect(requestTo("https://openrouter.ai/api/v1/chat/completions"))
                .andExpect(method(POST))
                .andExpect(content().string(containsString("\"name\":\"get_house_summary\"")))
                .andExpect(content().string(containsString("\"required\":[\"houseId\"]")))
                .andRespond(withSuccess(
                        "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":null,"
                                + "\"tool_calls\":[{\"id\":\"call_abc\",\"type\":\"function\","
                                + "\"function\":{\"name\":\"get_house_summary\","
                                + "\"arguments\":\"{\\\"houseId\\\":\\\"12\\\"}\"}}]}}]}",
                        MediaType.APPLICATION_JSON));

        AiPrediction prediction = client.chat(
                List.of(AiChatMessage.user("What is the summary for house 12?")),
                List.of(new AiToolDefinition("get_house_summary",
                        "Summarize a single house.",
                        Map.of("houseId", "ID of the house to summarize (required)"))),
                List.of("OWNER"));

        assertThat(prediction.toolCalls()).hasSize(1);
        AiToolCall call = prediction.toolCalls().get(0);
        assertThat(call.name()).isEqualTo("get_house_summary");
        assertThat(call.arguments()).containsEntry("houseId", "12");
    }

    @Test
    void missingModelReturnsConfigurationHintWithoutHttpCall() {
        MockRestServiceServer server = MockRestServiceServer.createServer(client.restTemplate());
        properties.setModel("");

        AiPrediction prediction =
                client.chat(List.of(AiChatMessage.user("hi")), List.of(), List.of("OWNER"));

        assertThat(prediction.content()).contains("OPENROUTER_MODEL");
        assertThat(prediction.toolCalls()).isEmpty();
        server.verify();
    }

    @Test
    void missingApiKeyReturnsConfigurationHintWithoutHttpCall() {
        MockRestServiceServer server = MockRestServiceServer.createServer(client.restTemplate());
        properties.setApiKey("");

        AiPrediction prediction =
                client.chat(List.of(AiChatMessage.user("hi")), List.of(), List.of("OWNER"));

        assertThat(prediction.content()).contains("OPENROUTER_API_KEY");
        assertThat(prediction.toolCalls()).isEmpty();
        server.verify();
    }

    @Test
    void unauthorizedReturnsApiKeyError() {
        MockRestServiceServer server = MockRestServiceServer.createServer(client.restTemplate());
        server.expect(requestTo("https://openrouter.ai/api/v1/chat/completions"))
                .andExpect(method(POST))
                .andRespond(withStatus(UNAUTHORIZED)
                        .body("{\"error\":{\"message\":\"Invalid API key\"}}")
                        .contentType(MediaType.APPLICATION_JSON));

        AiPrediction prediction =
                client.chat(List.of(AiChatMessage.user("hi")), List.of(), List.of("OWNER"));

        assertThat(prediction.content()).contains("Invalid API key");
        assertThat(prediction.toolCalls()).isEmpty();
    }

    @Test
    void tooManyRequestsReturnsRateLimitMessage() {
        MockRestServiceServer server = MockRestServiceServer.createServer(client.restTemplate());
        server.expect(requestTo("https://openrouter.ai/api/v1/chat/completions"))
                .andExpect(method(POST))
                .andRespond(withStatus(TOO_MANY_REQUESTS)
                        .body("{\"error\":{\"message\":\"Rate limit exceeded\"}}")
                        .contentType(MediaType.APPLICATION_JSON));

        AiPrediction prediction =
                client.chat(List.of(AiChatMessage.user("hi")), List.of(), List.of("OWNER"));

        assertThat(prediction.content()).contains("Too many requests");
        assertThat(prediction.toolCalls()).isEmpty();
    }

    @Test
    void modelNotFoundReturnsFriendlyReply() {
        MockRestServiceServer server = MockRestServiceServer.createServer(client.restTemplate());
        server.expect(requestTo("https://openrouter.ai/api/v1/chat/completions"))
                .andExpect(method(POST))
                .andRespond(withStatus(NOT_FOUND)
                        .body("{\"error\":{\"message\":\"Model not found\"}}")
                        .contentType(MediaType.APPLICATION_JSON));

        AiPrediction prediction =
                client.chat(List.of(AiChatMessage.user("hi")), List.of(), List.of("OWNER"));

        assertThat(prediction.content()).contains("unavailable");
        assertThat(prediction.toolCalls()).isEmpty();
    }

    @Test
    void malformedResponseReturnsFriendlyReply() {
        MockRestServiceServer server = MockRestServiceServer.createServer(client.restTemplate());
        server.expect(requestTo("https://openrouter.ai/api/v1/chat/completions"))
                .andExpect(method(POST))
                .andRespond(withSuccess("not json at all", MediaType.APPLICATION_JSON));

        AiPrediction prediction =
                client.chat(List.of(AiChatMessage.user("hi")), List.of(), List.of("OWNER"));

        assertThat(prediction.content()).contains("unreadable");
        assertThat(prediction.toolCalls()).isEmpty();
    }

    @Test
    void emptyChoicesReturnsFallbackReply() {
        MockRestServiceServer server = MockRestServiceServer.createServer(client.restTemplate());
        server.expect(requestTo("https://openrouter.ai/api/v1/chat/completions"))
                .andExpect(method(POST))
                .andRespond(withSuccess("{\"choices\":[]}", MediaType.APPLICATION_JSON));

        AiPrediction prediction =
                client.chat(List.of(AiChatMessage.user("hi")), List.of(), List.of("OWNER"));

        assertThat(prediction.content()).contains("unreadable");
        assertThat(prediction.toolCalls()).isEmpty();
    }

    @Test
    void sendsBearerAuthHeader() {
        MockRestServiceServer server = MockRestServiceServer.createServer(client.restTemplate());
        server.expect(requestTo("https://openrouter.ai/api/v1/chat/completions"))
                .andExpect(method(POST))
                .andExpect(header("Authorization", "Bearer test-api-key-12345"))
                .andExpect(header("HTTP-Referer", "https://teasjul.com"))
                .andExpect(header("X-Title", "TeasJul AI Assistant"))
                .andRespond(withSuccess(
                        "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"ok\"}}]}",
                        MediaType.APPLICATION_JSON));

        AiPrediction prediction =
                client.chat(List.of(AiChatMessage.user("hi")), List.of(), List.of("OWNER"));

        assertThat(prediction.content()).isEqualTo("ok");
    }

    @Test
    void toolResultMessageUsesToolCallId() {
        MockRestServiceServer server = MockRestServiceServer.createServer(client.restTemplate());
        server.expect(requestTo("https://openrouter.ai/api/v1/chat/completions"))
                .andExpect(method(POST))
                .andExpect(content().string(containsString("\"tool_call_id\":\"get_house_summary\"")))
                .andExpect(content().string(containsString("\"role\":\"tool\"")))
                .andRespond(withSuccess(
                        "{\"choices\":[{\"message\":{\"role\":\"assistant\","
                                + "\"content\":\"House summary ready.\"}}]}",
                        MediaType.APPLICATION_JSON));

        AiPrediction prediction = client.chat(
                List.of(
                        AiChatMessage.user("Show house 1"),
                        AiChatMessage.assistantCall(List.of(
                                new AiToolCall("get_house_summary", Map.of("houseId", "1")))),
                        AiChatMessage.toolResult("{\"name\":\"House 1\"}", "get_house_summary")),
                List.of(),
                List.of("OWNER"));

        assertThat(prediction.content()).isEqualTo("House summary ready.");
    }

    @Test
    void parsesToolCallEmbeddedAsJsonInContent() {
        MockRestServiceServer server = MockRestServiceServer.createServer(client.restTemplate());
        server.expect(requestTo("https://openrouter.ai/api/v1/chat/completions"))
                .andExpect(method(POST))
                .andRespond(withSuccess(
                        "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\""
                                + "{\\\"name\\\":\\\"get_vacant_rooms\\\","
                                + "\\\"arguments\\\":{\\\"houseId\\\":3}}\"}}]}",
                        MediaType.APPLICATION_JSON));

        AiPrediction prediction =
                client.chat(List.of(AiChatMessage.user("vacant rooms?")), List.of(), List.of("OWNER"));

        assertThat(prediction.toolCalls()).hasSize(1);
        AiToolCall call = prediction.toolCalls().get(0);
        assertThat(call.name()).isEqualTo("get_vacant_rooms");
        assertThat(call.arguments()).containsEntry("houseId", "3");
    }

    @Test
    void parsesFencedJsonToolCallInContent() {
        MockRestServiceServer server = MockRestServiceServer.createServer(client.restTemplate());
        server.expect(requestTo("https://openrouter.ai/api/v1/chat/completions"))
                .andExpect(method(POST))
                .andRespond(withSuccess(
                        "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\""
                                + "```json\\n{\\\"name\\\":\\\"tenant_balance\\\","
                                + "\\\"arguments\\\":{}}\\n```\"}}]}",
                        MediaType.APPLICATION_JSON));

        AiPrediction prediction =
                client.chat(List.of(AiChatMessage.user("balance")), List.of(), List.of("TENANT"));

        assertThat(prediction.toolCalls()).hasSize(1);
        assertThat(prediction.toolCalls().get(0).name()).isEqualTo("tenant_balance");
    }

    @Test
    void ordinaryReplyIsNotMistakenForToolCall() {
        MockRestServiceServer server = MockRestServiceServer.createServer(client.restTemplate());
        server.expect(requestTo("https://openrouter.ai/api/v1/chat/completions"))
                .andExpect(method(POST))
                .andRespond(withSuccess(
                        "{\"choices\":[{\"message\":{\"role\":\"assistant\","
                                + "\"content\":\"Sure, you have 3 houses.\"}}]}",
                        MediaType.APPLICATION_JSON));

        AiPrediction prediction =
                client.chat(List.of(AiChatMessage.user("how many houses")), List.of(), List.of("OWNER"));

        assertThat(prediction.content()).isEqualTo("Sure, you have 3 houses.");
        assertThat(prediction.toolCalls()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Streaming (chatStream) tests
    // -------------------------------------------------------------------------

    private static final MediaType TEXT_EVENT_STREAM =
            MediaType.parseMediaType("text/event-stream;charset=UTF-8");

    /** Builds a minimal SSE body with text delta frames and the terminal [DONE]. */
    private static String sseTextFrames(String... fragments) {
        StringBuilder sb = new StringBuilder();
        for (String frag : fragments) {
            sb.append("data: {\"choices\":[{\"delta\":{\"content\":\"")
              .append(frag.replace("\"", "\\\""))
              .append("\"}}]}\n\n");
        }
        sb.append("data: [DONE]\n\n");
        return sb.toString();
    }

    @Test
    void chatStream_plainTextDeltasAccumulated() {
        MockRestServiceServer server =
                MockRestServiceServer.createServer(client.streamRestTemplate());
        server.expect(requestTo("https://openrouter.ai/api/v1/chat/completions"))
                .andExpect(method(POST))
                .andExpect(content().string(containsString("\"stream\":true")))
                .andRespond(withSuccess(sseTextFrames("Hello", " there", "!"), TEXT_EVENT_STREAM));

        List<String> deltas = new java.util.ArrayList<>();
        AiPrediction result = client.chatStream(
                List.of(AiChatMessage.user("hi")), List.of(), List.of("OWNER"),
                deltas::add);

        assertThat(deltas).containsExactly("Hello", " there", "!");
        assertThat(result.content()).isEqualTo("Hello there!");
        assertThat(result.toolCalls()).isEmpty();
    }

    @Test
    void chatStream_toolCallFragmentsAssembled() {
        // Simulate streamed tool_calls: name in first frame, arguments split across two frames
        String frame1 = "data: {\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,"
                + "\"function\":{\"name\":\"get_house_summary\",\"arguments\":\"\"}}]}}]}\n\n";
        String frame2 = "data: {\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,"
                + "\"function\":{\"name\":\"\",\"arguments\":\"{\\\"houseId\\\":\\\"\"}}]}}]}\n\n";
        String frame3 = "data: {\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,"
                + "\"function\":{\"name\":\"\",\"arguments\":\"5\\\"}\"}}]}}]}\n\n";
        String done = "data: [DONE]\n\n";

        MockRestServiceServer server =
                MockRestServiceServer.createServer(client.streamRestTemplate());
        server.expect(requestTo("https://openrouter.ai/api/v1/chat/completions"))
                .andExpect(method(POST))
                .andRespond(withSuccess(frame1 + frame2 + frame3 + done, TEXT_EVENT_STREAM));

        List<String> deltas = new java.util.ArrayList<>();
        AiPrediction result = client.chatStream(
                List.of(AiChatMessage.user("house 5?")), List.of(), List.of("OWNER"),
                deltas::add);

        // No text deltas for tool-call-only frames
        assertThat(deltas).isEmpty();
        assertThat(result.toolCalls()).hasSize(1);
        AiToolCall call = result.toolCalls().get(0);
        assertThat(call.name()).isEqualTo("get_house_summary");
        assertThat(call.arguments()).containsEntry("houseId", "5");
    }

    @Test
    void chatStream_httpErrorReturnsFriendlyReply() {
        MockRestServiceServer server =
                MockRestServiceServer.createServer(client.streamRestTemplate());
        server.expect(requestTo("https://openrouter.ai/api/v1/chat/completions"))
                .andExpect(method(POST))
                .andRespond(withStatus(UNAUTHORIZED)
                        .body("{\"error\":{\"message\":\"Invalid API key\"}}")
                        .contentType(MediaType.APPLICATION_JSON));

        List<String> deltas = new java.util.ArrayList<>();
        AiPrediction result = client.chatStream(
                List.of(AiChatMessage.user("hi")), List.of(), List.of("OWNER"),
                deltas::add);

        assertThat(deltas).isEmpty();
        assertThat(result.content()).contains("Invalid API key");
        assertThat(result.toolCalls()).isEmpty();
    }

    @Test
    void chatStream_missingModelSkipsHttpCall() {
        MockRestServiceServer server =
                MockRestServiceServer.createServer(client.streamRestTemplate());
        properties.setModel("");

        List<String> deltas = new java.util.ArrayList<>();
        AiPrediction result = client.chatStream(
                List.of(AiChatMessage.user("hi")), List.of(), List.of("OWNER"),
                deltas::add);

        assertThat(deltas).isEmpty();
        assertThat(result.content()).contains("OPENROUTER_MODEL");
        server.verify(); // no HTTP call expected
    }
}

