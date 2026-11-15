package com.example.spring_boot_project_api.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Verifies that the {@code app.ai.provider} property selects the correct
 * {@link AiChatClient} bean without requiring a live LLM backend or database.
 */
class AiProviderSelectionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(UserConfigurations.of(AiProperties.class,
                    MockChatClient.class, OllamaChatClient.class,
                    OpenRouterChatClient.class));

    @Test
    void openrouterProviderActivatesOpenRouterChatClient() {
        runner.withPropertyValues("app.ai.provider=openrouter").run(context -> {
            assertThat(context).hasSingleBean(OpenRouterChatClient.class);
            assertThat(context).doesNotHaveBean(MockChatClient.class);
            assertThat(context).doesNotHaveBean(OllamaChatClient.class);
            assertThat(context).hasSingleBean(AiChatClient.class);
        });
    }

    @Test
    void mockProviderActivatesMockChatClient() {
        runner.withPropertyValues("app.ai.provider=mock").run(context -> {
            assertThat(context).hasSingleBean(MockChatClient.class);
            assertThat(context).doesNotHaveBean(OpenRouterChatClient.class);
            assertThat(context).doesNotHaveBean(OllamaChatClient.class);
            assertThat(context).hasSingleBean(AiChatClient.class);
        });
    }

    @Test
    void ollamaProviderActivatesOllamaChatClient() {
        runner.withPropertyValues("app.ai.provider=ollama").run(context -> {
            assertThat(context).hasSingleBean(OllamaChatClient.class);
            assertThat(context).doesNotHaveBean(OpenRouterChatClient.class);
            assertThat(context).doesNotHaveBean(MockChatClient.class);
            assertThat(context).hasSingleBean(AiChatClient.class);
        });
    }

    @Test
    void unknownProviderActivatesNoChatClient() {
        runner.withPropertyValues("app.ai.provider=unknown").run(context -> {
            assertThat(context).doesNotHaveBean(AiChatClient.class);
        });
    }
}