package com.example.agenticai.openai;

import com.example.agenticai.config.OpenAiProperties;
import com.example.agenticai.openai.model.ChatCompletionRequest;
import com.example.agenticai.openai.model.ChatCompletionResponse;
import com.example.agenticai.openai.model.ChatMessage;
import com.example.agenticai.openai.model.ToolDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);
    private final WebClient webClient;
    private final OpenAiProperties properties;

    public OpenAiClient(OpenAiProperties properties) {
        this.properties = properties;
        this.webClient = WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
        log.info("[OPENAI_CLIENT] Initialized | baseUrl='{}' | model='{}'",
                properties.getBaseUrl(), properties.getModel());
    }

    public ChatCompletionResponse chatCompletion(List<ChatMessage> messages, List<ToolDefinition> tools) {
        log.info("[OPENAI_API_CALL] ========== CALLING OPENAI API ==========");
        
        if (!properties.isConfigured()) {
            log.error("[OPENAI_API_CALL] Configuration Error: OPENAI_API_KEY is not set");
            throw new IllegalStateException(
                    "OPENAI_API_KEY is not set. Export it before starting the app, e.g. " +
                    "export OPENAI_API_KEY=sk-...");
        }

        log.info("[OPENAI_API_CALL] Request Details | messageCount={} | toolCount={} | model='{}'",
                messages.size(), tools.size(), properties.getModel());
        
        log.debug("[OPENAI_API_CALL] Messages sent to model:");
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage msg = messages.get(i);
            log.debug("[OPENAI_API_CALL]   Message {}: role='{}' | contentLength={} | toolCalls={}",
                    i, msg.role(), 
                    msg.content() != null ? msg.content().length() : 0,
                    msg.toolCalls() != null ? msg.toolCalls().size() : 0);
        }

        log.debug("[OPENAI_API_CALL] Tools available for model: {}", 
                tools.stream().map(t -> t.function().name()).toList());

        ChatCompletionRequest request = new ChatCompletionRequest(
                properties.getModel(),
                messages,
                tools,
                "auto",
                0.2
        );

        log.info("[OPENAI_API_CALL] Sending request to OpenAI API...");
        ChatCompletionResponse response = webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                        .map(body -> {
                            log.error("[OPENAI_API_CALL] API Error Response: {}", body);
                            return new IllegalStateException("OpenAI API error: " + body);
                        }))
                .bodyToMono(ChatCompletionResponse.class)
                .block();

        if (response != null && !response.choices().isEmpty()) {
            ChatCompletionResponse.Choice choice = response.choices().get(0);
            log.info("[OPENAI_API_CALL] API Response Received | finishReason='{}' | messageRole='{}' | contentLength={}",
                    choice.finishReason(), choice.message().role(),
                    choice.message().content() != null ? choice.message().content().length() : 0);
            
            if (choice.message().toolCalls() != null && !choice.message().toolCalls().isEmpty()) {
                log.info("[OPENAI_API_CALL] Model Decision: Requesting {} tool call(s) | tools: {}",
                        choice.message().toolCalls().size(),
                        choice.message().toolCalls().stream()
                                .map(tc -> tc.function().name())
                                .toList());
            } else {
                log.info("[OPENAI_API_CALL] Model Decision: Returning final answer (no tools requested)");
            }
        }

        log.info("[OPENAI_API_CALL] ========== OPENAI API CALL COMPLETED ==========");
        return response;
    }
}
