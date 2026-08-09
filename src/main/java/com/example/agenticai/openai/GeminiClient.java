package com.example.agenticai.openai;

import com.example.agenticai.config.GeminiProperties;
import com.example.agenticai.openai.model.ChatCompletionResponse;
import com.example.agenticai.openai.model.ChatMessage;
import com.example.agenticai.openai.model.ToolCall;
import com.example.agenticai.openai.model.ToolDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gemini API client for handling tool-calling requests with Google's Gemini model.
 * Converts OpenAI format to Gemini format and vice versa.
 */
@Component
@EnableConfigurationProperties(GeminiProperties.class)
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);
    private final WebClient webClient;
    private final GeminiProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiClient(GeminiProperties properties) {
        this.properties = properties;
        this.webClient = WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
        log.info("[GEMINI_CLIENT] Initialized | baseUrl='{}' | model='{}'",
                properties.getBaseUrl(), properties.getModel());
    }

    public ChatCompletionResponse chatCompletion(List<ChatMessage> messages, List<ToolDefinition> tools) {
        log.info("[GEMINI_API_CALL] ========== CALLING GEMINI API ==========");
        
        if (!properties.isConfigured()) {
            log.error("[GEMINI_API_CALL] Configuration Error: GEMINI_API_KEY is not set");
            throw new IllegalStateException("GEMINI_API_KEY is not set. Set it in application.properties or GEMINI_API_KEY environment variable");
        }

        log.info("[GEMINI_API_CALL] Request Details | messageCount={} | toolCount={} | model='{}'",
                messages.size(), tools.size(), properties.getModel());

        try {
            // Convert OpenAI format to Gemini format
            Map<String, Object> geminiRequest = convertToGeminiFormat(messages, tools);
            
            log.debug("[GEMINI_API_CALL] Sending request to Gemini API...");
            // Ensure endpoint is a relative path so WebClient uses the configured baseUrl.
            String endpoint = String.format("/%s:generateContent?key=%s",
                    properties.getModel(), properties.getApiKey());

            log.debug("[GEMINI_API_CALL] Full URL: {}{}", properties.getBaseUrl(), endpoint);

            String response = webClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(geminiRequest)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                            .map(body -> {
                                log.error("[GEMINI_API_CALL] API Error Response: {}", body);
                                return new IllegalStateException("Gemini API error: " + body);
                            }))
                    .bodyToMono(String.class)
                    .block();

            // Convert Gemini response back to OpenAI format
            ChatCompletionResponse openaiResponse = convertFromGeminiFormat(response);
            
            log.info("[GEMINI_API_CALL] ========== GEMINI API CALL COMPLETED ==========");
            return openaiResponse;

        } catch (Exception e) {
            log.error("[GEMINI_API_CALL] Error during Gemini API call: {}", e.getMessage(), e);
            throw new IllegalStateException("Gemini API call failed", e);
        }
    }

    private Map<String, Object> convertToGeminiFormat(List<ChatMessage> messages, List<ToolDefinition> tools) {
        Map<String, Object> request = new HashMap<>();
        
        // System prompt handling
        List<Map<String, Object>> contents = new ArrayList<>();
        for (ChatMessage msg : messages) {
            Map<String, Object> content = new HashMap<>();
            content.put("role", msg.role());
            
            if (msg.content() != null) {
                Map<String, String> part = new HashMap<>();
                part.put("text", msg.content());
                content.put("parts", List.of(part));
            }
            contents.add(content);
        }
        
        request.put("contents", contents);
        
        // Tool definitions: Gemini's expected schema differs from OpenAI's. For now, omit full tool definitions
        // to avoid invalid payload errors. Log a warning so callers know tool-calling is not forwarded.
        if (!tools.isEmpty()) {
            log.warn("[GEMINI_API_CALL] Tool definitions detected but Gemini tool-calling format not implemented. Omitting tools from request.");
            // Do not include any tools field to avoid API schema errors
        }
        
        // Generation config
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.2);
        request.put("generationConfig", generationConfig);
        
        return request;
    }

    private ChatCompletionResponse convertFromGeminiFormat(String geminiResponseJson) throws Exception {
        JsonNode root = objectMapper.readTree(geminiResponseJson);
        JsonNode candidates = root.get("candidates");
        
        if (candidates == null || !candidates.isArray() || candidates.size() == 0) {
            log.error("[GEMINI_API_CALL] No candidates in Gemini response");
            throw new IllegalStateException("No response from Gemini API");
        }

        JsonNode firstCandidate = candidates.get(0);
        JsonNode content = firstCandidate.get("content");
        JsonNode parts = content.get("parts");
        
        String responseText = "";
        if (parts != null && parts.isArray() && parts.size() > 0) {
            JsonNode firstPart = parts.get(0);
            if (firstPart.has("text")) {
                responseText = firstPart.get("text").asText();
            } else if (firstPart.has("functionCall")) {
                // Handle tool calls
                log.info("[GEMINI_API_CALL] Model Decision: Requesting tool call(s)");
            }
        }

        // Create OpenAI-compatible response
        ToolCall toolCall = null; // placeholder - handle functionCall -> ToolCall conversion if needed
        ChatMessage assistantMessage = new ChatMessage(
                "assistant",
                responseText,
                toolCall != null ? List.of(toolCall) : null,
                null,
                null
        );

        log.info("[GEMINI_API_CALL] Model Response Received | responseLength={}", responseText.length());
        
        // Return as ChatCompletionResponse (id, choices, usage)
        return new ChatCompletionResponse(
                properties.getModel(),
                List.of(new ChatCompletionResponse.Choice(0, assistantMessage, "stop")),
                null
        );
    }
}
