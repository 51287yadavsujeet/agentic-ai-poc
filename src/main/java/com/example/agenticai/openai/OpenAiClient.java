package com.example.agenticai.openai;

import com.example.agenticai.config.OpenAiProperties;
import com.example.agenticai.openai.model.ChatCompletionRequest;
import com.example.agenticai.openai.model.ChatCompletionResponse;
import com.example.agenticai.openai.model.ChatMessage;
import com.example.agenticai.openai.model.ToolDefinition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * Talks to OpenAI's /chat/completions endpoint with function-calling ("tools") enabled.
 * This is the only class that knows about the OpenAI wire format - everything else in the
 * app works against plain domain objects.
 */
@Component
public class OpenAiClient {

    private final WebClient webClient;
    private final OpenAiProperties properties;

    public OpenAiClient(OpenAiProperties properties) {
        this.properties = properties;
        this.webClient = WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    public ChatCompletionResponse chatCompletion(List<ChatMessage> messages, List<ToolDefinition> tools) {
        if (!properties.isConfigured()) {
            throw new IllegalStateException(
                    "OPENAI_API_KEY is not set. Export it before starting the app, e.g. " +
                    "export OPENAI_API_KEY=sk-...");
        }

        ChatCompletionRequest request = new ChatCompletionRequest(
                properties.getModel(),
                messages,
                tools,
                "auto",
                0.2
        );

        return webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                        .map(body -> new IllegalStateException("OpenAI API error: " + body)))
                .bodyToMono(ChatCompletionResponse.class)
                .block();
    }
}
