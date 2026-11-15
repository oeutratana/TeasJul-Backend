package com.example.spring_boot_project_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.spring_boot_project_api.config.AiProperties;
import com.example.spring_boot_project_api.config.JwtAuthenticationFilter;
import com.example.spring_boot_project_api.config.SecurityConfig;
import com.example.spring_boot_project_api.dto.request.AssistantChatRequest;
import com.example.spring_boot_project_api.dto.response.AssistantChatResponse;
import com.example.spring_boot_project_api.exception.AssistantDisabledException;
import com.example.spring_boot_project_api.service.AssistantService;
import com.example.spring_boot_project_api.service.assistant.AssistantStreamListener;
import com.example.spring_boot_project_api.service.assistant.dto.AiToolCall;
import com.example.spring_boot_project_api.util.JwtUtil;

@WebMvcTest(AssistantController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "jwt.expiration-ms=900000"
})
class AssistantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssistantService assistantService;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private AiProperties aiProperties;
    @MockitoBean(name = "streamExecutor")
    private Executor streamExecutor;

    private static final String BODY = "{\"message\":\"What is the summary for house 12?\"}";

    @Test
    void chatRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void chatReturnsAssistantResponseForAuthenticatedUser() throws Exception {
        when(aiProperties.isEnabled()).thenReturn(true);
        AiToolCall toolCall = new AiToolCall("get_house_summary", Map.of("houseId", "12"));
        when(assistantService.chat(any(AssistantChatRequest.class)))
                .thenReturn(new AssistantChatResponse("ok", List.of(toolCall), false));

        mockMvc.perform(post("/api/assistant/chat")
                        .with(user("sok").roles("OWNER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("ok"))
                .andExpect(jsonPath("$.error").value(false))
                .andExpect(jsonPath("$.toolCalls[0].name").value("get_house_summary"));

        verify(assistantService).chat(any(AssistantChatRequest.class));
    }

    @Test
    void chatRejectsBlankMessage() throws Exception {
        mockMvc.perform(post("/api/assistant/chat")
                        .with(user("sok").roles("OWNER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chatRejectsMalformedBody() throws Exception {
        mockMvc.perform(post("/api/assistant/chat")
                        .with(user("sok").roles("OWNER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chatForbiddenWhenAssistantDisabled() throws Exception {
        when(assistantService.chat(any(AssistantChatRequest.class)))
                .thenThrow(new AssistantDisabledException());

        mockMvc.perform(post("/api/assistant/chat")
                        .with(user("sok").roles("OWNER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("The AI assistant is currently disabled."));
    }

    // -------------------------------------------------------------------------
    // Streaming endpoint tests
    // -------------------------------------------------------------------------

    @Test
    void streamRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/assistant/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void streamForbiddenWhenAssistantDisabled() throws Exception {
        // isEnabled() is checked on the request thread — throws before emitter is created
        when(aiProperties.isEnabled()).thenReturn(false);

        mockMvc.perform(post("/api/assistant/chat/stream")
                        .with(user("sok").roles("OWNER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void streamStartsAsyncForAuthenticatedUser() throws Exception {
        when(aiProperties.isEnabled()).thenReturn(true);
        when(aiProperties.getReadTimeoutSeconds()).thenReturn(180);

        // Make the executor run the Runnable synchronously so the test can observe it,
        // but have chatStream immediately call onDone to complete the emitter.
        doAnswer(inv -> {
            Runnable task = inv.getArgument(0);
            // Run synchronously; the listener will call emitter.complete()
            doAnswer(svcInv -> {
                AssistantStreamListener listener = svcInv.getArgument(1);
                listener.onDone(new AssistantChatResponse("streaming done", List.of(), false));
                return null;
            }).when(assistantService).chatStream(any(), any());
            task.run();
            return null;
        }).when(streamExecutor).execute(any());

        mockMvc.perform(post("/api/assistant/chat/stream")
                        .with(user("sok").roles("OWNER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
    }
}