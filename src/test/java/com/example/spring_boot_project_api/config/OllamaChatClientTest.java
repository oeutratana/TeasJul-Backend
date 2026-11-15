package com.example.spring_boot_project_api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
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
 * Unit tests for the Ollama provider against a mocked HTTP backend; no live
 * Ollama server is required.
 */
class OllamaChatClientTest {

    private AiProperties properties;
    private OllamaChatClient client;

    @BeforeEach
    void setUp() {
        properties = new AiProperties();
        properties.setBaseUrl("http://localhost:11434");
        properties.setModel("qwen3:4b");
        properties.setReadTimeoutSeconds(180);
        client = new OllamaChatClient(properties);
    }

    @Test
    void returnsPlainReplyWhenNoToolCalls() {
        MockRestServiceServer server = MockRestServiceServer.createServer(client.restTemplate());
        server.expect(requestTo("http://localhost:11434/api/chat"))
                .andExpect(method(POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess(
                        "{\"message\":{\"role\":\"assistant\",\"content\":\"Hello there!\","
                                + "\"tool_calls\":[]}}",
                        MediaType.APPLICATION_JSON));

        AiPrediction prediction =
                client.chat(List.of(AiChatMessage.user("hi")), List.of(), List.of("OWNER"));

        assertThat(prediction.content()).isEqualTo("Hello there!");
        assertThat(prediction.toolCalls()).isEmpty();
    }

    @Test
    void parsesToolCallsAndStringifiesNonStringArguments() {
        MockRestServiceServer server = MockRestServiceServer.createServer(client.restTemplate());
        server.expect(requestTo("http://localhost:11434/api/chat"))
                .andExpect(method(POST))
                .andExpect(content().string(containsString("\"name\":\"get_house_summary\"")))
                .andExpect(content().string(containsString("\"required\":[\"houseId\"]")))
                .andExpect(content().string(containsString("\"stream\":false")))
                .andRespond(withSuccess(
                        "{\"message\":{\"role\":\"assistant\",\"content\":\"\","
                                + "\"tool_calls\":[{\"function\":{\"name\":\"get_house_summary\","
                                + "\"arguments\":{\"houseId\":12}}}]}}",
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
        properties.setModel("mock");

        AiPrediction prediction =
                client.chat(List.of(AiChatMessage.user("hi")), List.of(), List.of("OWNER"));

        assertThat(prediction.content()).contains("AI_MODEL");
        assertThat(prediction.toolCalls()).isEmpty();
        server.verify();
    }

    @Test
    void modelNotFoundReturnsFriendlyReply() {
        MockRestServiceServer server = MockRestServiceServer.createServer(client.restTemplate());
        server.expect(requestTo("http://localhost:11434/api/chat"))
                .andExpect(method(POST))
                .andRespond(withStatus(NOT_FOUND)
                        .body("{\"error\":\"model 'qwen3:4b' not found, try pulling it first\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        AiPrediction prediction =
                client.chat(List.of(AiChatMessage.user("hi")), List.of(), List.of("OWNER"));

        assertThat(prediction.content()).contains("unavailable");
        assertThat(prediction.toolCalls()).isEmpty();
    }

    @Test
    void malformedResponseReturnsFriendlyReply() {
        MockRestServiceServer server = MockRestServiceServer.createServer(client.restTemplate());
        server.expect(requestTo("http://localhost:11434/api/chat"))
                .andExpect(method(POST))
                .andRespond(withSuccess("not json at all", MediaType.APPLICATION_JSON));

        AiPrediction prediction =
                client.chat(List.of(AiChatMessage.user("hi")), List.of(), List.of("OWNER"));

        assertThat(prediction.content()).contains("unreadable");
        assertThat(prediction.toolCalls()).isEmpty();
    }

    @Test
    void parsesToolCallEmbeddedAsJsonInContent() {
        MockRestServiceServer server = MockRestServiceServer.createServer(client.restTemplate());
        server.expect(requestTo("http://localhost:11434/api/chat"))
                .andExpect(method(POST))
                .andRespond(withSuccess(
                        "{\"message\":{\"role\":\"assistant\",\"content\":\""
                                + "{\\\"name\\\":\\\"get_vacant_rooms\\\","
                                + "\\\"arguments\\\":{\\\"houseId\\\":3}}\"}}",
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
        server.expect(requestTo("http://localhost:11434/api/chat"))
                .andExpect(method(POST))
                .andRespond(withSuccess(
                        "{\"message\":{\"role\":\"assistant\",\"content\":\""
                                + "```json\\n{\\\"name\\\":\\\"tenant_balance\\\","
                                + "\\\"arguments\\\":{}}\\n```\"}}",
                        MediaType.APPLICATION_JSON));

        AiPrediction prediction =
                client.chat(List.of(AiChatMessage.user("balance")), List.of(), List.of("TENANT"));

        assertThat(prediction.toolCalls()).hasSize(1);
        assertThat(prediction.toolCalls().get(0).name()).isEqualTo("tenant_balance");
    }

    @Test
    void ordinaryReplyIsNotMistakenForToolCall() {
        MockRestServiceServer server = MockRestServiceServer.createServer(client.restTemplate());
        server.expect(requestTo("http://localhost:11434/api/chat"))
                .andExpect(method(POST))
                .andRespond(withSuccess(
                        "{\"message\":{\"role\":\"assistant\",\"content\":\""
                                + "Sure, you have 3 houses.\"}}",
                        MediaType.APPLICATION_JSON));

        AiPrediction prediction =
                client.chat(List.of(AiChatMessage.user("how many houses")), List.of(), List.of("OWNER"));

        assertThat(prediction.content()).isEqualTo("Sure, you have 3 houses.");
        assertThat(prediction.toolCalls()).isEmpty();
    }
}